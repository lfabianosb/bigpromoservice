package flight.latam;

import java.io.IOException;

import org.apache.http.client.CookieStore;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import flight.model.Flight;
import flight.voegol.exception.SearchException;

public class Latam {

	private static final String CHARSET = "UTF-8";
	private static final int CONNECTION_TIMEOUT = Integer.parseInt(System.getenv("CONNECTION_TIMEOUT"));
	private static final String URL_BASE = "https://book.latam.com";
	private static final String URL = URL_BASE + "/TAM/dyn/air/booking/upslDispatcher";

	private Flight flight;

	public Latam(Flight flight) {
		this.flight = flight;
	}

	/**
	 * Realizar a pesquisa
	 * 
	 * @return Voo encontrado
	 * @throws SearchException
	 */
	public Flight search() throws SearchException {
		String dateDep = flight.getDateDeparture().substring(6) + flight.getDateDeparture().substring(3, 5)
				+ flight.getDateDeparture().substring(0, 2) + "0000";
		String dateRet = flight.getDateReturn().substring(6) + flight.getDateReturn().substring(3, 5)
				+ flight.getDateReturn().substring(0, 2) + "0000";

		String params = "?B_LOCATION_1=" + flight.getFrom() + "&E_LOCATION_1=" + flight.getTo()
				+ "&TRIP_TYPE=R&B_DATE_1=" + dateDep + "&B_DATE_2=" + dateRet + "&adults=" + flight.getAdult()
				+ "&children=" + flight.getChild()
				+ "&infants=0&LANGUAGE=BR&SITE=JJBKJJBK&WDS_MARKET=BR&MARKETING_CABIN=E";

		CloseableHttpClient httpClient = buildHttpClient();
		HttpGet httpGet = new HttpGet(URL + params);
		CloseableHttpResponse response = null;
		String htmlPage = null;
		try {
			response = httpClient.execute(httpGet);
			htmlPage = EntityUtils.toString(response.getEntity(), CHARSET);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (htmlPage == null || htmlPage.length() == 0) {
			throw new SearchException("Não houve resposta do endereço " + URL + params);
		}

		Document doc = Jsoup.parse(htmlPage, URL_BASE);
		Elements trechos = doc.select("section.calendarPricesSection");

		if (trechos.size() != 2) {
			throw new SearchException("Ida ou volta não encontrada");
		}

		boolean ida = true;
		for (Element trecho : trechos) {
			Elements prices = trecho.select("ul.selected .tc");
			for (Element price : prices) {
				// TODO Ajustar a conversão
				float prc = Float.parseFloat(price.text().replace(".", "").replace(",", "."));
				if (ida) {
					flight.setPriceDeparture(prc);
				} else {
					flight.setPriceReturn(prc);
				}
			}

			ida = false;
		}

		return flight;
	}

	/**
	 * Configurar o cliente HTTP que fará o request
	 * 
	 * @return Cliente HTTP configurado
	 */
	private CloseableHttpClient buildHttpClient() {
		RequestConfig globalConfig = RequestConfig.custom()
				.setCookieSpec(CookieSpecs.DEFAULT)
				.setConnectionRequestTimeout(CONNECTION_TIMEOUT)
				.setConnectTimeout(CONNECTION_TIMEOUT)
				.setSocketTimeout(CONNECTION_TIMEOUT).
				build();

		CookieStore cookieStore = new BasicCookieStore();

		return HttpClients.custom().setDefaultRequestConfig(globalConfig).setDefaultCookieStore(cookieStore).build();
	}

}
