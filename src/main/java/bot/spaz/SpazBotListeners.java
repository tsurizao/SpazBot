package bot.spaz;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class SpazBotListeners extends ListenerAdapter {
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!event.getAuthor().isBot()) {
            String messageSent = event.getMessage().getContentRaw();
            event.getChannel().sendMessage("You said: " + messageSent).queue();
        }
    }
}
