package error.irc;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class IrcUser {
    private String username;
    private String role;
    private int color;
    private boolean online;
}
