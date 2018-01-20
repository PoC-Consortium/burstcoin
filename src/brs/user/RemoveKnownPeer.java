package brs.user;

import brs.peer.Peer;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.InetAddress;

import static brs.http.common.Parameters.PEER_PARAMETER;
import static brs.user.JSONResponses.LOCAL_USERS_ONLY;

public final class RemoveKnownPeer extends UserServlet.UserRequestHandler {

  static final RemoveKnownPeer instance = new RemoveKnownPeer();

  private RemoveKnownPeer() {}

  @Override
  JSONStreamAware processRequest(HttpServletRequest req, User user) throws IOException {
    if (Users.allowedUserHosts == null && ! InetAddress.getByName(req.getRemoteAddr()).isLoopbackAddress()) {
      return LOCAL_USERS_ONLY;
    } else {
      int index = Integer.parseInt(req.getParameter(PEER_PARAMETER));
      Peer peer = Users.getPeer(index);
      if (peer != null) {
        peer.remove();
      }
    }
    return null;
  }
}
