package error.irc;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IrcMessage {
    private final IrcUser sender;
    private final String content;
    private final long timestamp;
}
