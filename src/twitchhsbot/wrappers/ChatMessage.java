/*
 */

package twitchhsbot.wrappers;

public class ChatMessage {
    
    public String channel;
    public String sender;
    public String text;
    
    public ChatMessage(String channel, String sender, String text) {
        this.channel = channel;
        this.sender = sender;
        this.text = text;
    }

}
