package net.floodlightcontroller.reactive;

import java.io.IOException;
import java.util.ArrayList;

import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.MappingJsonFactory;

public class ReactivePusher extends ServerResource {
	@Post
	public String store(String json) throws IOException {
		MappingJsonFactory f = new MappingJsonFactory();
		JsonParser jp;
		ArrayList<FlowMap> map = new ArrayList<>();

		try {
			jp = f.createParser(json);
		} catch (JsonParseException e) {
			throw new IOException(e);
		}

		jp.nextToken();
		if (jp.getCurrentToken() != JsonToken.START_OBJECT) {
			throw new IOException("Expected START_OBJECT");
		}

		jp.nextToken();
		if (jp.getCurrentToken() != JsonToken.FIELD_NAME) {
			throw new IOException("Expected FIELD_NAME");
		}

		jp.nextToken();
		if (jp.getCurrentToken() != JsonToken.START_ARRAY) {
			throw new IOException("Expected START_ARRAY");
		}

		while (jp.nextToken() != JsonToken.END_ARRAY) {
			FlowMap m = new FlowMap();

			if (jp.getCurrentToken() != JsonToken.START_OBJECT) {
				throw new IOException("Expected START_OBJECT");
			}

			jp.nextToken();
			if (jp.getCurrentToken() != JsonToken.FIELD_NAME) {
				throw new IOException("Expected FIELD_NAME");
			}

			while (jp.getCurrentToken() != JsonToken.END_OBJECT) {
				if (jp.getCurrentToken() != JsonToken.FIELD_NAME) {
					throw new IOException("Expected FIELD_NAME");
				}
				String n = jp.getCurrentName();

				jp.nextToken();

				switch (n) {
				case "name":
					m.setName(jp.getText());
					jp.nextToken();
					break;
				case "active":
					m.setActive(jp.getText());
					jp.nextToken();
					break;
				case "idle_timeout":
					m.setIdle_timeout(jp.getText());
					jp.nextToken();
					break;
				case "hard_timeout":
					m.setHard_timeout(jp.getText());
					jp.nextToken();
					break;
				case "priority":
					m.setPriority(jp.getText());
					jp.nextToken();
					break;
				case "cookie":
					m.setCookie(jp.getText());
					jp.nextToken();
					break;
				case "eth_type":
					m.setEth_type(jp.getText());
					jp.nextToken();
					break;
				case "ip_proto":
					m.setIp_proto(jp.getText());
					jp.nextToken();
					break;
				case "tp_src":
					m.setTp_src(jp.getText());
					jp.nextToken();
					break;
				case "tp_dst":
					m.setTp_dst(jp.getText());
					jp.nextToken();
					break;
				case "ipv4_src":
					m.setIpv4_src(jp.getText());
					jp.nextToken();
					break;
				case "ipv4_dst":
					m.setIpv4_dst(jp.getText());
					jp.nextToken();
					break;
				case "eth_src":
					m.setEth_src(jp.getText());
					jp.nextToken();
					break;
				case "eth_dst":
					m.setEth_dst(jp.getText());
					jp.nextToken();
					break;
				case "dpid":
					m.setDPID(jp.getText());
					jp.nextToken();
					break;
				case "out_port":
					m.setOut_port(jp.getText());
					jp.nextToken();
					break;
				case "in_port":
					m.setIn_port(jp.getText());
					jp.nextToken();
					break;
				case "hibrido":
					m.setHibrido(jp.getText());
					jp.nextToken();
					break;
				default:
					jp.nextToken();
					break;
				}
			}
			map.add(m);
		}
		Reactive.setMap(map);
		return ("{\"status\" : \"" + "Pushed" + "\"}");
	}
}
