import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.*;
import com.amazon.speech.ui.*;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

public class CoinCheckSpeechlet implements Speechlet {

    private static final String COIN = "coin";

    static String stringFromJson(String urlString) throws Exception {
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuilder buffer = new StringBuilder();
            int read;
            char[] chars = new char[1024];
            while ((read = reader.read(chars)) != -1)
                buffer.append(chars, 0, read);

            // Prints entire JSON for debugging purposes
            // System.out.println(buffer.toString());

            return buffer.substring(1, buffer.length() - 1);
        } catch (Exception e) {
            System.out.println(e.getStackTrace());
            return null;
        } finally {
            if (reader != null)
                reader.close();
        }
    }

    static JSONObject coinmarketAPI(String coin) throws Exception {

        String urlString = "https://api.coinmarketcap.com/v1/ticker/" + coin;

        JSONObject coinData = new JSONObject(stringFromJson(urlString));

        return coinData;
    }


    // called when session first starts
    @Override
    public void onSessionStarted(final SessionStartedRequest request, final Session session)
            throws SpeechletException {
    }

    // "Alexa, ask CoinCheck about ${coin}"
    @Override
    public SpeechletResponse onLaunch(final LaunchRequest request, final Session session)
            throws SpeechletException {
        return getWelcomeResponse();
    }


    static String formatUSDString(String usd) {
        StringBuilder usdBuilder = new StringBuilder();
        usdBuilder.append(usd.substring(0, usd.indexOf('.')));
        usdBuilder.append(" dollars and ");

        String cents = usd.substring(usd.indexOf('.') + 1, usd.indexOf('.') + 3) + '.'
                + usd.substring(usd.indexOf('.') + 3, usd.length());

        usdBuilder.append(cents + " cents");

        return usdBuilder.toString();
    }

    @Override
    public void onSessionEnded(final SessionEndedRequest request, final Session session)
            throws SpeechletException {
        // any cleanup logic goes here
    }

    private SpeechletResponse getWelcomeResponse() {
        String speechOutput = "I am Coin Check. Which coin do you want information for?";
        // If the user either does not reply to the welcome message or says something that is not
        // understood, they will be prompted again with this text.
        String repromptText =
                "With Coin Check, you can get data for any cryptocurrency supported by CoinMarketCap.com. "
                        + " For example, you could say Bitcoin, or Ethereum.."
                        + " Now, which coin do you want, player?";

        return newAskResponse(speechOutput, false, repromptText, false);
    }

    // "Alexa, open CoinCheck"
    @Override
    public SpeechletResponse onIntent(final IntentRequest request, final Session session)
            throws SpeechletException {

        Intent intent = request.getIntent();
        String intentName = (intent != null) ? intent.getName() : null;

        if ("GetCoinIntent".equals(intentName)) {
            try {
                return getCoinDataResponse(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if ("AMAZON.HelpIntent".equals(intentName)) {
            return getHelpResponse();

        } else if ("AMAZON.StopIntent".equals(intentName)) {
            PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
            outputSpeech.setText("Stay thirsty, my friends.");

            return SpeechletResponse.newTellResponse(outputSpeech);
        } else if ("AMAZON.CancelIntent".equals(intentName)) {
            PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
            outputSpeech.setText("Stay thirsty, my friends.");

            return SpeechletResponse.newTellResponse(outputSpeech);
        } else {
            throw new SpeechletException("Invalid Intent");
        }

        PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
        outputSpeech.setText("There was an error of some sort. I'm sorry I wasted your time. I'm just a machine.");

        return SpeechletResponse.newTellResponse(outputSpeech);
    }

    /**
     * Gets a coin from the API and returns the data to the user.
     */
    private SpeechletResponse getCoinDataResponse(Intent intent) throws Exception {

        Slot coinSlot = intent.getSlot("coin");
        String coin = coinSlot.getValue().replace(' ', '-');
        JSONObject coinData = coinmarketAPI(coin); // will this work

        String coin_name = coinData.getString("name");
        String usd_value = coinData.getString("price_usd");
        String percent_change_1h = coinData.getString("percent_change_1h");
        String percent_change_24h = coinData.getString("percent_change_24h");
        String percent_change_7d = coinData.getString("percent_change_7d");

        StringBuilder speechBuilder = new StringBuilder();

        speechBuilder.append("Here is the current information for " + coin_name + ". ");
        speechBuilder.append("The current value is " + formatUSDString(usd_value) + ". "); // TODO: String to money
        speechBuilder.append("The percent change in the past one hour is " + percent_change_1h + " percent. ");
        speechBuilder.append("The percent change in the past 24 hours is " + percent_change_24h + " percent. ");
        speechBuilder.append("The percent change in the past 7 days is " + percent_change_7d + " percent. ");

        // Create the Simple card content.
        SimpleCard card = new SimpleCard();
        card.setTitle("CoinCheck");
        card.setContent(speechBuilder.toString());

        // Create the plain text output.
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechBuilder.toString());

        return SpeechletResponse.newTellResponse(speech, card);
    }

    /**
     * Returns a response for the help intent.
     */
    private SpeechletResponse getHelpResponse() {
        String speechText =
                "You can ask Coin Check to check on a coin for you. Which coin do you want?";

        // Create the plain text output.
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechText);

        // Create reprompt
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(speech);

        return SpeechletResponse.newAskResponse(speech, reprompt);
    }

    /**
     * Wrapper for creating the Ask response from the input strings.
     *
     * @param stringOutput   the output to be spoken
     * @param isOutputSsml   whether the output text is of type SSML
     * @param repromptText   the reprompt for if the user doesn't reply or is misunderstood.
     * @param isRepromptSsml whether the reprompt text is of type SSML
     * @return SpeechletResponse the speechlet response
     */
    private SpeechletResponse newAskResponse(String stringOutput, boolean isOutputSsml,
                                             String repromptText, boolean isRepromptSsml) {
        OutputSpeech outputSpeech, repromptOutputSpeech;
        if (isOutputSsml) {
            outputSpeech = new SsmlOutputSpeech();
            ((SsmlOutputSpeech) outputSpeech).setSsml(stringOutput);
        } else {
            outputSpeech = new PlainTextOutputSpeech();
            ((PlainTextOutputSpeech) outputSpeech).setText(stringOutput);
        }

        if (isRepromptSsml) {
            repromptOutputSpeech = new SsmlOutputSpeech();
            ((SsmlOutputSpeech) repromptOutputSpeech).setSsml(repromptText);
        } else {
            repromptOutputSpeech = new PlainTextOutputSpeech();
            ((PlainTextOutputSpeech) repromptOutputSpeech).setText(repromptText);
        }
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(repromptOutputSpeech);
        return SpeechletResponse.newAskResponse(outputSpeech, reprompt);
    }
}
