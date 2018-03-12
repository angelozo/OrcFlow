package net.floodlightcontroller.reactive;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import net.floodlightcontroller.restserver.RestletRoutable;

public class ReactiveWebRoutable implements RestletRoutable {

	@Override
	public Restlet getRestlet(Context context) {
		Router router = new Router(context);
		router.attach("/json", ReactivePusher.class);
		return router;
	}

	@Override
	public String basePath() {
		return "/reactive/flowpusher";
	}

}
