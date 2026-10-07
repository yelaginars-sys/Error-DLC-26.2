package error.account;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Microsoft OAuth authentication for Minecraft via Device Code Flow:
 * 1. Request device code from Microsoft OAuth
 * 2. Opens verification url in user's browser (e.g. https://microsoft.com/link) and copies user_code to clipboard
 * 3. Polls token endpoint asynchronously
 * 4. Acquires Xbox Live (XBL), XSTS, and Minecraft tokens
 * 5. Fetches Minecraft profile and sets session
 */
public final class MicrosoftAuth {

    private static final String CLIENT_ID = "00000000402b5328"; // Standard Minecraft Client ID
    private static final String SCOPE = "service::user.auth.xboxlive.com::MBI_SSL";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public record AuthResult(boolean success, String username, UUID uuid, String accessToken, String error) {}

    public static CompletableFuture<AuthResult> loginWithBrowser(Consumer<String> statusCallback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                statusCallback.accept("Запрос кода авторизации...");

                // Step 1: Request Device Code
                String body = "client_id=" + CLIENT_ID + "&scope=" + URLEncoder.encode(SCOPE, StandardCharsets.UTF_8);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    return new AuthResult(false, null, null, null, "Ошибка получения кода: HTTP " + response.statusCode());
                }

                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                String userCode = json.get("user_code").getAsString();
                String deviceCode = json.get("device_code").getAsString();
                String verificationUri = json.get("verification_uri").getAsString();
                int interval = json.has("interval") ? json.get("interval").getAsInt() : 5;
                int expiresIn = json.has("expires_in") ? json.get("expires_in").getAsInt() : 900;

                // Copy code to clipboard & open browser
                try {
                    java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(userCode), null);
                } catch (Exception ignored) {}

                try {
                    if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                        java.awt.Desktop.getDesktop().browse(URI.create(verificationUri));
                    }
                } catch (Exception ignored) {}

                statusCallback.accept("Код " + userCode + " скопирован! Авторизуйтесь на сайте...");

                // Step 2: Poll for token
                long startTime = System.currentTimeMillis();
                String msAccessToken = null;

                while (System.currentTimeMillis() - startTime < expiresIn * 1000L) {
                    Thread.sleep(Math.max(interval, 3) * 1000L);

                    String pollBody = "grant_type=urn:ietf:params:oauth:grant_type:device_code"
                            + "&client_id=" + CLIENT_ID
                            + "&device_code=" + deviceCode;

                    HttpRequest pollReq = HttpRequest.newBuilder()
                            .uri(URI.create("https://login.microsoftonline.com/consumers/oauth2/v2.0/token"))
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString(pollBody))
                            .build();

                    HttpResponse<String> pollRes = HTTP_CLIENT.send(pollReq, HttpResponse.BodyHandlers.ofString());
                    if (pollRes.statusCode() == 200) {
                        JsonObject pollJson = JsonParser.parseString(pollRes.body()).getAsJsonObject();
                        msAccessToken = pollJson.get("access_token").getAsString();
                        break;
                    } else {
                        JsonObject errJson = JsonParser.parseString(pollRes.body()).getAsJsonObject();
                        String error = errJson.has("error") ? errJson.get("error").getAsString() : "";
                        if ("authorization_declined".equals(error) || "bad_device_code".equals(error)) {
                            return new AuthResult(false, null, null, null, "Авторизация отклонена пользователем");
                        }
                    }
                }

                if (msAccessToken == null) {
                    return new AuthResult(false, null, null, null, "Таймаут ожидания авторизации");
                }

                statusCallback.accept("Вход в Xbox Live...");

                // Step 3: Xbox Live Authentication
                JsonObject xblReqJson = new JsonObject();
                JsonObject xblProps = new JsonObject();
                xblProps.addProperty("AuthMethod", "RPS");
                xblProps.addProperty("SiteName", "user.auth.xboxlive.com");
                xblProps.addProperty("RpsTicket", "d=" + msAccessToken);
                xblReqJson.add("Properties", xblProps);
                xblReqJson.addProperty("RelyingParty", "http://auth.xboxlive.com");
                xblReqJson.addProperty("TokenType", "JWT");

                HttpRequest xblReq = HttpRequest.newBuilder()
                        .uri(URI.create("https://user.auth.xboxlive.com/user/authenticate"))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(xblReqJson.toString()))
                        .build();

                HttpResponse<String> xblRes = HTTP_CLIENT.send(xblReq, HttpResponse.BodyHandlers.ofString());
                if (xblRes.statusCode() != 200) {
                    return new AuthResult(false, null, null, null, "Ошибка Xbox Live: HTTP " + xblRes.statusCode());
                }

                JsonObject xblJson = JsonParser.parseString(xblRes.body()).getAsJsonObject();
                String xblToken = xblJson.get("Token").getAsString();
                String userHash = xblJson.getAsJsonObject("DisplayClaims").getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs").getAsString();

                statusCallback.accept("Получение XSTS токена...");

                // Step 4: XSTS Token
                JsonObject xstsReqJson = new JsonObject();
                JsonObject xstsProps = new JsonObject();
                xstsProps.addProperty("SandboxId", "RETAIL");
                com.google.gson.JsonArray userTokens = new com.google.gson.JsonArray();
                userTokens.add(xblToken);
                xstsProps.add("UserTokens", userTokens);
                xstsReqJson.add("Properties", xstsProps);
                xstsReqJson.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
                xstsReqJson.addProperty("TokenType", "JWT");

                HttpRequest xstsReq = HttpRequest.newBuilder()
                        .uri(URI.create("https://xsts.auth.xboxlive.com/xsts/authorize"))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(xstsReqJson.toString()))
                        .build();

                HttpResponse<String> xstsRes = HTTP_CLIENT.send(xstsReq, HttpResponse.BodyHandlers.ofString());
                if (xstsRes.statusCode() != 200) {
                    return new AuthResult(false, null, null, null, "Ошибка XSTS: HTTP " + xstsRes.statusCode());
                }

                JsonObject xstsJson = JsonParser.parseString(xstsRes.body()).getAsJsonObject();
                String xstsToken = xstsJson.get("Token").getAsString();

                statusCallback.accept("Вход в сервисы Minecraft...");

                // Step 5: Minecraft Services Login
                JsonObject mcLoginJson = new JsonObject();
                mcLoginJson.addProperty("identityToken", "XBL3.0 x=" + userHash + ";" + xstsToken);

                HttpRequest mcLoginReq = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.minecraftservices.com/authentication/login_with_xbox"))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(mcLoginJson.toString()))
                        .build();

                HttpResponse<String> mcLoginRes = HTTP_CLIENT.send(mcLoginReq, HttpResponse.BodyHandlers.ofString());
                if (mcLoginRes.statusCode() != 200) {
                    return new AuthResult(false, null, null, null, "Ошибка авторизации Minecraft: HTTP " + mcLoginRes.statusCode());
                }

                JsonObject mcLoginObj = JsonParser.parseString(mcLoginRes.body()).getAsJsonObject();
                String mcAccessToken = mcLoginObj.get("access_token").getAsString();

                statusCallback.accept("Загрузка профиля игрока...");

                // Step 6: Profile Query
                HttpRequest profileReq = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.minecraftservices.com/minecraft/profile"))
                        .header("Authorization", "Bearer " + mcAccessToken)
                        .GET()
                        .build();

                HttpResponse<String> profileRes = HTTP_CLIENT.send(profileReq, HttpResponse.BodyHandlers.ofString());
                if (profileRes.statusCode() != 200) {
                    return new AuthResult(false, null, null, null, "У вас нет купленной копии Minecraft на этом аккаунте!");
                }

                JsonObject profileJson = JsonParser.parseString(profileRes.body()).getAsJsonObject();
                String profileName = profileJson.get("name").getAsString();
                String rawUuid = profileJson.get("id").getAsString();

                // Format raw 32-char UUID string into standard UUID with dashes
                UUID profileUuid;
                try {
                    String formatted = rawUuid.replaceFirst(
                            "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                            "$1-$2-$3-$4-$5"
                    );
                    profileUuid = UUID.fromString(formatted);
                } catch (Exception e) {
                    profileUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + profileName).getBytes(StandardCharsets.UTF_8));
                }

                statusCallback.accept("Успешный вход: " + profileName);
                return new AuthResult(true, profileName, profileUuid, mcAccessToken, null);

            } catch (Exception e) {
                return new AuthResult(false, null, null, null, "Исключение: " + e.getMessage());
            }
        });
    }
}
