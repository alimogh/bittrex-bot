package br.com.bittrexbot.rest.client;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import br.com.bittrexbot.rest.model.v3.MarketSummary;
import br.com.bittrexbot.rest.model.v3.MarketTicker;
import br.com.bittrexbot.rest.model.v3.Order;
import br.com.bittrexbot.rest.model.v3.Order.Direction;
import br.com.bittrexbot.rest.model.v3.Order.TimeInForce;
import br.com.bittrexbot.rest.model.v3.Order.Type;
import br.com.bittrexbot.rest.model.v3.OrderResponse;
import br.com.bittrexbot.utils.Global;

@Component
public class BittrexClientV3 {

	public MarketSummary[] getMarketSummaries(){
		String URL = "https://api.bittrex.com/v3/markets/summaries";
		return new RestTemplate().getForObject(URL, MarketSummary[].class);
	}
	
	public MarketSummary getMarketSummary(String market){
		String URL = "https://api.bittrex.com/v3/markets/"+market+"/summary";
		return new RestTemplate().getForObject(URL, MarketSummary.class);
	}
	
	public MarketTicker[] getMarketTicker(){
		String URL = "https://api.bittrex.com/v3/markets/tickers";
		return new RestTemplate().getForObject(URL, MarketTicker[].class);
	}
	
	public MarketTicker getMarketTicker(String market){
		String URL = "https://api.bittrex.com/v3/markets/"+market+"/ticker";
		return new RestTemplate().getForObject(URL, MarketTicker.class);
	}
	
	public OrderResponse order(String market, Direction direction, Double quantity) {
		
		try {
			
			//Market Order ONLY
			String URL = "https://api.bittrex.com/v3/orders";
			
			Order order = new Order(market, direction, Type.MARKET, quantity, TimeInForce.IMMEDIATE_OR_CANCEL);
			ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
			String jsonContent = ow.writeValueAsString(order);
			
			HttpEntity<String> requestEntity = new HttpEntity<>(jsonContent, getApiSignature(URL, jsonContent, HttpMethod.POST));
			
			return new RestTemplate().exchange(URL, HttpMethod.POST, requestEntity, OrderResponse.class).getBody();
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("error on create order: "+e.getMessage());
		}
	}
	
	public HttpHeaders getApiSignature(String URL, String content, HttpMethod http){
		
		HttpHeaders headers = new HttpHeaders();
		try {
			
			String sign = null;
			String contentHash = DigestUtils.sha512Hex(content);
			
			OffsetDateTime utc = OffsetDateTime.now(ZoneOffset.UTC);
			String timestamp = String.valueOf(Date.from(utc.toInstant()).getTime());
			
			try {
				
				String preSign = timestamp+URL+http.name()+contentHash+"";
				Mac mac = Mac.getInstance("HmacSHA512");
				SecretKeySpec secret = new SecretKeySpec(Global.SECRET.getBytes(),"HmacSHA512");
				mac.init(secret);
				byte[] digest = mac.doFinal(preSign.getBytes());
				sign = org.apache.commons.codec.binary.Hex.encodeHexString(digest);
				
			} catch (Exception e) {
				throw new RuntimeException("error on create the apisignature: "+e.getMessage());
			}
			
			headers.add("Api-Key", Global.KEY);
			headers.add("Api-Timestamp", timestamp);
			headers.add("Api-Content-Hash", contentHash);
			headers.add("Api-Signature", sign);
			headers.add("Content-Type", "application/json");
			
		} catch (Exception e) {
			throw new RuntimeException("error on create the apisignature: "+e.getMessage());
		}
		
		return headers;
	}
}
