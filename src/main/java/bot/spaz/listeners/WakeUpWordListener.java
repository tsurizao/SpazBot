package bot.spaz.listeners;

import ai.picovoice.porcupine.Porcupine;
import ai.picovoice.porcupine.PorcupineException;
import ignored.PicoToken;
import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.CombinedAudio;
import net.dv8tion.jda.api.audio.UserAudio;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Text;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class WakeUpWordListener extends ListenerAdapter implements AudioReceiveHandler {

    private TextChannel textChannel;
    private final VoiceChannel voiceChannel;
    private static Porcupine porcupineINSTANCE;

    // Getter for the porcupineINSTANCE
    public static Porcupine getPorcupineINSTANCE() {
        return porcupineINSTANCE;
    }

    // Getter for the text channel
    public TextChannel getTextChannel() {
        return textChannel;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        this.textChannel = event.getChannel().asTextChannel();
    }

    // Constructor builds the porcupine instance
    public WakeUpWordListener(VoiceChannel voiceChannel) throws PorcupineException {
        this.voiceChannel = voiceChannel;
        porcupineINSTANCE = new Porcupine.Builder()
                .setAccessKey(PicoToken.getToken())
                .setBuiltInKeywords(
                        new Porcupine.BuiltInKeyword[]
                                {
                                        Porcupine.BuiltInKeyword.JARVIS,
                                        Porcupine.BuiltInKeyword.BUMBLEBEE
                                })
                .build();
    }

    // Run ... what I believe should work
    public void Run() {
        try {
            voiceChannel.getGuild().getAudioManager().setReceivingHandler(new AudioReceiveHandler() {
                @Override
                public boolean canReceiveCombined() {
                    return false;
                }

                @Override
                public boolean canReceiveUser() {
                    return true;
                }

                @Override
                public void handleCombinedAudio(@NotNull CombinedAudio combinedAudio) {
                    // Leave blank, no action taken for combined audio
                }

                @Override
                public void handleUserAudio(@NotNull UserAudio userAudio) {
                    try {
                        processIncomingUserAudioData(userAudio.getAudioData(1));
                    } catch (PorcupineException | IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } catch (Exception e) {
            System.out.println("Error with Run()");
            e.printStackTrace();
        }
    }

    private AudioInputStream inputStreamForReading(AudioInputStream sourceStream) {
        final AudioFormat sourceFormat = sourceStream.getFormat();

        final AudioFormat to16khzFormat = new AudioFormat(
                sourceFormat.getEncoding(),
                16000f,
                sourceFormat.getSampleSizeInBits(),
                sourceFormat.getChannels(),
                sourceFormat.getFrameSize(),
                512f,
                sourceFormat.isBigEndian()
        );

        final AudioInputStream resampled16khz = AudioSystem.getAudioInputStream(to16khzFormat, sourceStream);

        final AudioFormat toMonoLEFormat = new AudioFormat(
                to16khzFormat.getEncoding(),
                to16khzFormat.getSampleRate(),
                to16khzFormat.getSampleSizeInBits(),
                1,
                to16khzFormat.getFrameSize() / 2,
                to16khzFormat.getFrameRate(),
                false
        );

        return AudioSystem.getAudioInputStream(toMonoLEFormat, resampled16khz);
    }

    public void processIncomingUserAudioData(byte[] bytes) throws PorcupineException, IOException {
        if (porcupineINSTANCE == null) {
            return;
        }

        byte[] byteBuffer = new byte[porcupineINSTANCE.getFrameLength() * 2];
        short[] shortBuffer = new short[porcupineINSTANCE.getFrameLength()];

        // Calling the AudioInputStream method to read data
        inputStreamForReading(new AudioInputStream(new ByteArrayInputStream(bytes), AudioReceiveHandler.OUTPUT_FORMAT, bytes.length))
                .read(byteBuffer, 0, byteBuffer.length);

        ByteBuffer.wrap(byteBuffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortBuffer);

        try {
            int keyword = porcupineINSTANCE.process(shortBuffer);
            if (keyword == 0) {
                System.out.println("JARVIS");
            }
            if (keyword == 1) {
                System.out.println("BUMBLEBEE");
            }
        } catch (Exception e) {
            System.out.println("Error processing shortBuffer in covertToShortArray");
            e.printStackTrace();
        }
    }
}