package net.floodlightcontroller.reactive;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.TCP;
import net.floodlightcontroller.packet.UDP;
import net.floodlightcontroller.restserver.IRestApiService;
import sun.net.www.protocol.http.HttpURLConnection;

public class Reactive implements IOFMessageListener, IFloodlightModule {
	protected IFloodlightProviderService floodlightProvider;
	protected static Logger logger;
	protected IRestApiService restApiService;
	protected IOFSwitchService switchService;
	private static ArrayList<FlowMap> map = new ArrayList<>();
	private Ethernet ultimoEth;
	private IPv4 ultimoIPv4;
	private TCP ultimoTCP;
	private UDP ultimoUDP;

	@Override
	public String getName() {
		return Reactive.class.getSimpleName();
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		return false;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		// Adiciona o REST API ao modulo
		l.add(IRestApiService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		logger = LoggerFactory.getLogger(Reactive.class);
		// Inicializa o REST API
		restApiService = context.getServiceImpl(IRestApiService.class);
		switchService = context.getServiceImpl(IOFSwitchService.class);
	}

	@Override
	public void startUp(FloodlightModuleContext context) {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		// Registra o REST API
		restApiService.addRestletRoutable(new ReactiveWebRoutable());
	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw, OFMessage msg,
			FloodlightContext cntx) {

		// Verifica se a tabela esta vazia
		if (map.isEmpty()) {
			// logger.info("A tabela Reactive esta vazia!");
			return Command.CONTINUE;
		}

		switch (msg.getType()) {
		case PACKET_IN:
			Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
			OFPacketIn pi = (OFPacketIn) msg;

			if (eth.getEtherType() == EthType.IPv4) {
				IPv4 ipv4 = (IPv4) eth.getPayload();

				if (ultimoEth != null && ultimoEth.getDestinationMACAddress().equals(eth.getDestinationMACAddress())
						&& ultimoEth.getSourceMACAddress().equals(eth.getSourceMACAddress()))
					if (ultimoIPv4 != null && ultimoIPv4.getDestinationAddress().equals(ipv4.getDestinationAddress())
							&& ultimoIPv4.getSourceAddress().equals(ipv4.getSourceAddress())
							&& ipv4.getProtocol().equals(ipv4.getProtocol()))
						if (ipv4.getProtocol().equals(IpProtocol.TCP)) {
							TCP tcp = (TCP) ipv4.getPayload();
							if (ultimoTCP != null && ultimoTCP.getDestinationPort().equals(tcp.getDestinationPort())
									&& ultimoTCP.getSourcePort().equals(tcp.getSourcePort()))
								return Command.CONTINUE;
						} else if (ipv4.getProtocol().equals(IpProtocol.UDP)) {
							UDP udp = (UDP) ipv4.getPayload();
							if (ultimoUDP != null && ultimoUDP.getDestinationPort().equals(udp.getDestinationPort())
									&& ultimoUDP.getSourcePort().equals(udp.getSourcePort()))
								return Command.CONTINUE;
						}

				ultimoEth = eth;
				ultimoIPv4 = ipv4;
				for (int i = 0; i < map.size(); i++) {
					boolean proto = true;

					if (map.get(i).getIpv4_src() != null) {
						if (map.get(i).getIpv4_dst() != null) {
							if (ipv4.getDestinationAddress().equals(map.get(i).getIpv4_dst())
									&& ipv4.getSourceAddress().equals(map.get(i).getIpv4_src())) {
								if (map.get(i).getIp_proto() != null
										&& !map.get(i).getIp_proto().equals(IpProtocol.of((short) 0))) {
									proto = ipv4.getProtocol().equals(map.get(i).getIp_proto());
								}

								if (proto) {
									boolean in_port = true;
									boolean out_port = true;
									if (ipv4.getProtocol().equals(IpProtocol.TCP)) {
										TCP tcp = (TCP) ipv4.getPayload();
										ultimoTCP = tcp;

										if (map.get(i).getTp_src() != null) {
											in_port = tcp.getSourcePort().equals(map.get(i).getTp_src());
										}
										if (map.get(i).getTp_dst() != null) {
											out_port = tcp.getDestinationPort().equals(map.get(i).getTp_dst());
										}
									} else if (ipv4.getProtocol().equals(IpProtocol.UDP)) {
										UDP udp = (UDP) ipv4.getPayload();
										ultimoUDP = udp;

										if (map.get(i).getTp_src() != null) {
											in_port = udp.getSourcePort().equals(map.get(i).getTp_src());
										}
										if (map.get(i).getTp_dst() != null) {
											out_port = udp.getDestinationPort().equals(map.get(i).getTp_dst());
										}
									}

									if (out_port && in_port) {
										map.get(i).setBuffer(pi.getBufferId());
										try {

											if (map.get(i).getHibrido().equals("true"))
												try {
													proativo(map.get(i).getName());
												} catch (Exception e) {
													e.printStackTrace();
												}
											createRoute(map.get(i), true);
											createRoute(map.get(i), false);

										} catch (Exception e) {
											e.printStackTrace();
										}
									}
								}
							} else if (ipv4.getSourceAddress().equals(map.get(i).getIpv4_dst())
									&& ipv4.getDestinationAddress().equals(map.get(i).getIpv4_src())) {
								if (map.get(i).getIp_proto() != null
										&& !map.get(i).getIp_proto().equals(IpProtocol.of((short) 0))) {
									proto = ipv4.getProtocol().equals(map.get(i).getIp_proto());
								}

								if (proto) {
									boolean in_port = true;
									boolean out_port = true;
									if (ipv4.getProtocol().equals(IpProtocol.TCP)) {
										TCP tcp = (TCP) ipv4.getPayload();
										ultimoTCP = tcp;

										if (map.get(i).getTp_src() != null) {
											in_port = tcp.getDestinationPort().equals(map.get(i).getTp_src());
										}
										if (map.get(i).getTp_dst() != null) {
											out_port = tcp.getSourcePort().equals(map.get(i).getTp_dst());
										}
									} else if (ipv4.getProtocol().equals(IpProtocol.UDP)) {
										UDP udp = (UDP) ipv4.getPayload();
										ultimoUDP = udp;

										if (map.get(i).getTp_src() != null) {
											in_port = udp.getDestinationPort().equals(map.get(i).getTp_src());
										}
										if (map.get(i).getTp_dst() != null) {
											out_port = udp.getSourcePort().equals(map.get(i).getTp_dst());
										}
									}

									if (out_port && in_port) {
										map.get(i).setBuffer(pi.getBufferId());
										try {
											if (map.get(i).getHibrido().equals("true"))
												try {
													proativo(map.get(i).getName());
												} catch (Exception e) {
													e.printStackTrace();
												}
											createRoute(map.get(i), true);
											createRoute(map.get(i), false);
										} catch (Exception e) {
											e.printStackTrace();
										}
									}
								}
							}
						}
					}

				}
				// logger.info("A rota para {} nao consta na tabela!",
				// ipv4.getDestinationAddress().toString());
			}
			break;
		default:
			break;
		}
		return Command.CONTINUE;
	}

	private void proativo(String name) throws Exception {
//		new Thread() {
//			@Override
//			public void run() {
				try {
					String url = "http://localhost:8081/OrcFlow/api/hibrido";
					URL obj = new URL(url);
					HttpURLConnection con = (HttpURLConnection) obj.openConnection();

					// add reuqest header
					con.setRequestMethod("POST");
					con.setRequestProperty("User-Agent", "Mozilla/5.0");
					con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
					con.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");

					// Send post request
					con.setDoOutput(true);
					DataOutputStream wr = new DataOutputStream(con.getOutputStream());
					wr.writeBytes(name.substring(1));
					wr.flush();
					wr.close();

					BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
					String inputLine;
					StringBuffer response = new StringBuffer();

					while ((inputLine = in.readLine()) != null) {
						response.append(inputLine);
					}
					in.close();

					// print result
					// System.out.println(response.toString());
				} catch (Exception e) {
					e.printStackTrace();
				}
//			}
//		}.start();

	}

	private void createRoute(FlowMap m, boolean ida) throws UnknownHostException {
		if (ida) {
			if (m.getDPID() != null) {
				System.out.println(m.getDPID());
				DatapathId dpid = m.getDPID();
				IOFSwitch sw = switchService.getSwitch(dpid);

				if (sw == null) {
					if (logger.isWarnEnabled()) {
						// logger.warn("Unable to push route, switch at DPID {}
						// " + "not available", dpid);
					}
				} else {

					OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();
					OFActionOutput.Builder aob = sw.getOFFactory().actions().buildOutput();
					List<OFAction> actions = new ArrayList<OFAction>();
					Match.Builder mb = sw.getOFFactory().buildMatch();

					if (EthType.IPv4.equals(m.getEth_type())) {
						IPv4Address src = m.getIpv4_src();
						IPv4Address dst = m.getIpv4_dst();

						mb.setExact(MatchField.ETH_TYPE, EthType.IPv4);
						mb.setExact(MatchField.IPV4_DST, dst);
						mb.setExact(MatchField.IPV4_SRC, src);
						mb.setExact(MatchField.ETH_SRC, m.getEth_src());
						mb.setExact(MatchField.ETH_DST, m.getEth_dst());

						if (IpProtocol.TCP.equals(m.getIp_proto())) {
							if (m.getTp_src() != null) {
								mb.setExact(MatchField.TCP_SRC, m.getTp_src());
							}
							if (m.getTp_dst() != null) {
								mb.setExact(MatchField.TCP_DST, m.getTp_dst());
							}

							mb.setExact(MatchField.IP_PROTO, IpProtocol.TCP);
						} else if (IpProtocol.UDP.equals(m.getIp_proto())) {
							if (m.getTp_src() != null) {
								mb.setExact(MatchField.UDP_SRC, m.getTp_src());
							}
							if (m.getTp_dst() != null) {
								mb.setExact(MatchField.UDP_DST, m.getTp_dst());
							}

							mb.setExact(MatchField.IP_PROTO, IpProtocol.UDP);
						}
					} else if (EthType.IPv6.equals(m.getEth_type())) {
						mb.setExact(MatchField.ETH_TYPE, EthType.IPv6);
					}

					if (m.getOut_port() != null) {
						OFPort outPort = m.getOut_port();
						aob.setPort(outPort);
						aob.setMaxLen(Integer.MAX_VALUE);
						actions.add(aob.build());

						fmb.setActions(actions);
						fmb.setMatch(mb.build());
						fmb.setBufferId(m.getBuffer());
						fmb.setCookie(m.getCookie());
						fmb.setOutPort(outPort);
						fmb.setPriority(m.getPriority());
						if (m.getIdle_timeout() != null) {
							fmb.setIdleTimeout(m.getIdle_timeout());
						}
						if (m.getHard_timeout() != null) {
							fmb.setIdleTimeout(m.getHard_timeout());
						}

						sw.write(fmb.build());
					}
				}
			}
		} else {
			if (m.getDPID() != null) {
				DatapathId dpid = m.getDPID();
				IOFSwitch sw = switchService.getSwitch(dpid);

				if (sw == null) {
					if (logger.isWarnEnabled()) {
						// logger.warn("Unable to push route, switch at DPID {}
						// " + "not available", dpid);
					}
				} else {

					OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();
					OFActionOutput.Builder aob = sw.getOFFactory().actions().buildOutput();
					List<OFAction> actions = new ArrayList<OFAction>();
					Match.Builder mb = sw.getOFFactory().buildMatch();

					if (EthType.IPv4.equals(m.getEth_type())) {
						IPv4Address src = m.getIpv4_src();
						IPv4Address dst = m.getIpv4_dst();

						mb.setExact(MatchField.ETH_TYPE, EthType.IPv4);
						mb.setExact(MatchField.IPV4_SRC, dst);
						mb.setExact(MatchField.IPV4_DST, src);
						mb.setExact(MatchField.ETH_DST, m.getEth_src());
						mb.setExact(MatchField.ETH_SRC, m.getEth_dst());

						if (IpProtocol.TCP.equals(m.getIp_proto())) {
							if (m.getTp_src() != null)
								mb.setExact(MatchField.TCP_DST, m.getTp_src());

							if (m.getTp_dst() != null)
								mb.setExact(MatchField.TCP_SRC, m.getTp_dst());

							mb.setExact(MatchField.IP_PROTO, IpProtocol.TCP);
						} else if (IpProtocol.UDP.equals(m.getIp_proto())) {
							if (m.getTp_src() != null) {
								mb.setExact(MatchField.UDP_DST, m.getTp_src());
							}
							if (m.getTp_dst() != null) {
								mb.setExact(MatchField.UDP_SRC, m.getTp_dst());
							}

							mb.setExact(MatchField.IP_PROTO, IpProtocol.UDP);
						}
					} else if (EthType.IPv6.equals(m.getEth_type())) {
						mb.setExact(MatchField.ETH_TYPE, EthType.IPv6);
					}

					if (m.getIn_port() != null) {
						OFPort outPort = m.getIn_port();
						aob.setPort(outPort);
						aob.setMaxLen(Integer.MAX_VALUE);
						actions.add(aob.build());

						fmb.setActions(actions);
						fmb.setMatch(mb.build());
						fmb.setBufferId(m.getBuffer());
						fmb.setCookie(m.getCookie());
						fmb.setOutPort(outPort);
						fmb.setPriority(m.getPriority());
						if (m.getIdle_timeout() != null) {
							fmb.setIdleTimeout(m.getIdle_timeout());
						}
						if (m.getHard_timeout() != null) {
							fmb.setIdleTimeout(m.getHard_timeout());
						}

						sw.write(fmb.build());
					}
				}
			}
		}
	}

	// Recebe a tabela
	public static void setMap(ArrayList<FlowMap> map) {
		Reactive.map = map;
	}

}
