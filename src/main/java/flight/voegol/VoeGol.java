package flight.voegol;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import flight.model.Flight;
import flight.voegol.exception.SearchException;

public class VoeGol {
	private static final String CHARSET = "UTF-8";
	private static final String URL_GET = "http://compre2.voegol.com.br/Select2.aspx";
	private static final String URL_POST = "http://compre2.voegol.com.br/CSearch.aspx?culture=pt-br";
	private static final String UNAVAILABLE = "indisponível";
	private static final int CONNECTION_TIMEOUT = 15000; // 15s

	private Flight flight;

	public VoeGol(Flight flight) {
		this.flight = flight;
	}

	/**
	 * Realizar a pesquisa
	 * @return Voo encontrado
	 * @throws SearchException
	 */
	public Flight search() throws SearchException {
		HttpPost post = configSearch();

		CloseableHttpClient client = buildHttpClient();
		Document document = null;
		InputStream content = null;
		try {
			// Faz esta requisição para setar os parâmetros na sessão
			client.execute(post);

			HttpResponse response = client.execute(new HttpGet(URL_GET));
			content = response.getEntity().getContent();
			document = Jsoup.parse(content, CHARSET, URL_GET);
		} catch (Exception e) {
			throw new SearchException(e);
		} finally {
			if (content != null) {
				try {
					content.close();
				} catch (IOException e) {
				}
			}

			if (client != null) {
				try {
					client.close();
				} catch (IOException e) {
				}
			}
		}

		return findFlight(document);
	}

	/**
	 * Configurar a pesquisa via post HTTP
	 * @return Post HTTP configurado
	 * @throws SearchException
	 */
	private HttpPost configSearch() throws SearchException {
		HttpPost post = new HttpPost(URL_POST);
		try {
			addParameters(post);
		} catch (UnsupportedEncodingException e) {
			throw new SearchException(e);
		}

		return post;
	}

	/**
	 * Procurar os voos da ida e volta dado um document HTML
	 * @param document
	 * @return Voo encontrado no documento
	 * @throws SearchException
	 */
	private Flight findFlight(Document document) throws SearchException {
		Elements trechos = document.select("li.active .price");

		if (trechos.size() != 2) {
			throw new SearchException("Ida ou volta não encontrada");
		}

		boolean ida = true;
		for (Element trecho : trechos) {
			if (UNAVAILABLE.equalsIgnoreCase(trecho.text())) {
				if (ida) {
					throw new SearchException("IDA indisponível");
				} else {
					throw new SearchException("VOLTA indisponível");
				}
			} else {
				Element elmPrice = trecho.select("span span:last-child").first();
				if (elmPrice.text() != null && elmPrice.text().length() > 0) {
					float price = Float.parseFloat(elmPrice.text().replace(".", "").replace(",", "."));
					if (ida) {
						flight.setPriceDeparture(price);
					} else {
						flight.setPriceReturn(price);
					}
				}

			}
			ida = false;
		}

		return flight;
	}

	/**
	 * Configurar o cliente HTTP que fará o request
	 * @return Cliente HTTP configurado
	 */
	private CloseableHttpClient buildHttpClient() {
		RequestConfig globalConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.DEFAULT)
				.setConnectionRequestTimeout(CONNECTION_TIMEOUT).setConnectTimeout(CONNECTION_TIMEOUT)
				.setSocketTimeout(CONNECTION_TIMEOUT).build();
		CookieStore cookieStore = new BasicCookieStore();

		return HttpClients.custom().setDefaultRequestConfig(globalConfig).setDefaultCookieStore(cookieStore).build();
	}

	/**
	 * Adicionar os parâmetros no post HTTP
	 * @param post
	 * @throws UnsupportedEncodingException
	 */
	private void addParameters(HttpPost post) throws UnsupportedEncodingException {
		String dayDep = flight.getDateDeparture().substring(0, 2); // dd
		String yearMonthDep = flight.getDateDeparture().substring(6) + "-" + flight.getDateDeparture().substring(3, 5); // yyyy-MM

		String dayRet = flight.getDateReturn().substring(0, 2); // dd
		String yearMonthRet = flight.getDateReturn().substring(6) + "-" + flight.getDateReturn().substring(3, 5); // yyyy-MM

		List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
		urlParameters.add(new BasicNameValuePair("header-chosen-origin", ""));
		urlParameters.add(new BasicNameValuePair("destiny-hidden", "false"));
		urlParameters.add(new BasicNameValuePair("header-chosen-destiny", ""));
		urlParameters.add(new BasicNameValuePair("goBack", "goAndBack"));
		urlParameters.add(new BasicNameValuePair("promotional-code", ""));
		urlParameters.add(new BasicNameValuePair(
				"ControlGroupSearchView$AvailabilitySearchInputSearchView$TextBoxMarketOrigin1", flight.getFrom()));
		urlParameters.add(new BasicNameValuePair(
				"ControlGroupSearchView$AvailabilitySearchInputSearchView$TextBoxMarketDestination1", flight.getTo()));
		urlParameters.add(new BasicNameValuePair(
				"ControlGroupSearchView$AvailabilitySearchInputSearchView$DropDownListMarketDay1", dayDep));
		urlParameters.add(new BasicNameValuePair(
				"ControlGroupSearchView$AvailabilitySearchInputSearchView$DropDownListMarketMonth1", yearMonthDep));
		urlParameters.add(new BasicNameValuePair(
				"ControlGroupSearchView$AvailabilitySearchInputSearchView$DropDownListMarketDay2", dayRet));
		urlParameters.add(new BasicNameValuePair(
				"ControlGroupSearchView$AvailabilitySearchInputSearchView$DropDownListMarketMonth2", yearMonthRet));
		urlParameters.add(new BasicNameValuePair(
				"ControlGroupSearchView$AvailabilitySearchInputSearchView$DropDownListPassengerType_ADT",
				Integer.toString(flight.getAdult())));
		urlParameters.add(new BasicNameValuePair(
				"ControlGroupSearchView$AvailabilitySearchInputSearchView$DropDownListPassengerType_CHD",
				Integer.toString(flight.getChild())));
		urlParameters.add(new BasicNameValuePair(
				"ControlGroupSearchView$AvailabilitySearchInputSearchView$DropDownListPassengerType_INFT", "0"));
		urlParameters.add(new BasicNameValuePair(
				"ControlGroupSearchView$AvailabilitySearchInputSearchView$RadioButtonMarketStructure", "RoundTrip"));
		urlParameters.add(new BasicNameValuePair(
				"ControlGroupSearchView$AvailabilitySearchInputSearchView$DropDownListResidentCountry", "br"));
		urlParameters.add(new BasicNameValuePair("PageFooter_SearchView$DropDownListOriginCountry", "pt"));
		urlParameters.add(new BasicNameValuePair("ControlGroupSearchView$ButtonSubmit", "compre aqui"));
		urlParameters.add(new BasicNameValuePair("size", "small"));
		urlParameters.add(new BasicNameValuePair("color", "default"));
		urlParameters.add(new BasicNameValuePair("__EVENTARGUMENT", ""));
		urlParameters.add(new BasicNameValuePair("__EVENTTARGET", ""));
		post.setEntity(new UrlEncodedFormEntity(urlParameters));
	}

}
