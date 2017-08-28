import org.junit.Test;

public class CoinCheckTest {

    @Test
    public void priceTest() throws Exception {
        System.out.println(CoinCheckSpeechlet.coinmarketAPI("Ethereum"));
    }

    @Test
    public void stringJson() throws Exception {
        CoinCheckSpeechlet.stringFromJson("https://api.coinmarketcap.com/v1/ticker/ethereum");
    }

    @Test
    public void usdFormatTest() {
        System.out.println(CoinCheckSpeechlet.formatUSDString("345.12345"));
    }
}
