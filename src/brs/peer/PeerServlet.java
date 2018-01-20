package brs.peer;

import brs.util.CountingInputStream;
import brs.util.CountingOutputStream;
import brs.util.JSON;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.servlets.gzip.CompressedResponseWrapper;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static brs.Constants.*;
import static brs.http.common.Parameters.HALLMARK_PARAMETER;
import static brs.http.common.Parameters.REQUEST_TYPE_PARAMETER;
import static brs.http.common.ResultFields.ERROR_RESPONSE;

public final class PeerServlet extends HttpServlet {

  private static final Logger logger = LoggerFactory.getLogger(PeerServlet.class);

  abstract static class PeerRequestHandler {
    abstract JSONStreamAware processRequest(JSONObject request, Peer peer);
  }

  private static final Map<String,PeerRequestHandler> peerRequestHandlers;

  static {
    final Map<String,PeerRequestHandler> map = new HashMap<>();
    map.put("addPeers", AddPeers.instance);
    map.put("getCumulativeDifficulty", GetCumulativeDifficulty.instance);
    map.put("getInfo", GetInfo.instance);
    map.put("getMilestoneBlockIds", GetMilestoneBlockIds.instance);
    map.put("getNextBlockIds", GetNextBlockIds.instance);
    map.put("getNextBlocks", GetNextBlocks.instance);
    map.put("getPeers", GetPeers.instance);
    map.put("getUnconfirmedTransactions", GetUnconfirmedTransactions.instance);
    map.put("processBlock", ProcessBlock.instance);
    map.put("processTransactions", ProcessTransactions.instance);
    map.put("getAccountBalance", GetAccountBalance.instance);
    map.put("getAccountRecentTransactions", GetAccountRecentTransactions.instance);
    peerRequestHandlers = Collections.unmodifiableMap(map);
  }

  private static final JSONStreamAware UNSUPPORTED_REQUEST_TYPE;
  static {
    final JSONObject response = new JSONObject();
    response.put(ERROR_RESPONSE, "Unsupported request type!");
    UNSUPPORTED_REQUEST_TYPE = JSON.prepare(response);
  }

  private static final JSONStreamAware UNSUPPORTED_PROTOCOL;
  static {
    final JSONObject response = new JSONObject();
    response.put(ERROR_RESPONSE, "Unsupported protocol!");
    UNSUPPORTED_PROTOCOL = JSON.prepare(response);
  }

  private boolean isGzipEnabled;

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    isGzipEnabled = Boolean.parseBoolean(config.getInitParameter("isGzipEnabled"));
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

    PeerImpl peer = null;
    JSONStreamAware response;

    try {
      peer = Peers.addPeer(req.getRemoteAddr(), null);
      if (peer == null) {
        return;
      }
      if (peer.isBlacklisted()) {
        return;
      }

      JSONObject request;
      CountingInputStream cis = new CountingInputStream(req.getInputStream());
      try (Reader reader = new InputStreamReader(cis, "UTF-8")) {
        request = (JSONObject) JSONValue.parse(reader);
      }
      if (request == null) {
        return;
      }

      if (peer.getState() == Peer.State.DISCONNECTED) {
        peer.setState(Peer.State.CONNECTED);
        if (peer.getAnnouncedAddress() != null) {
          Peers.updateAddress(peer);
        }
      }
      peer.updateDownloadedVolume(cis.getCount());
      if (! peer.analyzeHallmark(peer.getPeerAddress(), (String)request.get(HALLMARK_PARAMETER))) {
        peer.blacklist();
        return;
      }

      if (request.get(PROTOCOL) != null && ((String)request.get(PROTOCOL)).equals("B1")) {
        PeerRequestHandler peerRequestHandler = peerRequestHandlers.get(request.get(REQUEST_TYPE_PARAMETER));
        if (peerRequestHandler != null) {
          response = peerRequestHandler.processRequest(request, peer);
        } else {
          response = UNSUPPORTED_REQUEST_TYPE;
        }
      } else {
        logger.debug("Unsupported protocol " + request.get(PROTOCOL));
        response = UNSUPPORTED_PROTOCOL;
      }

    } catch (RuntimeException e) {
      logger.debug("Error processing POST request", e);
      JSONObject json = new JSONObject();
      json.put(ERROR_RESPONSE, e.toString());
      response = json;
    }

    resp.setContentType("text/plain; charset=UTF-8");
    try {
      long byteCount;
      if (isGzipEnabled) {
        try (Writer writer = new OutputStreamWriter(resp.getOutputStream(), "UTF-8")) {
          response.writeJSONString(writer);
        }
        byteCount = ((Response) ((CompressedResponseWrapper) resp).getResponse()).getContentCount();
      } else {
        CountingOutputStream cos = new CountingOutputStream(resp.getOutputStream());
        try (Writer writer = new OutputStreamWriter(cos, "UTF-8")) {
          response.writeJSONString(writer);
        }
        byteCount = cos.getCount();
      }
      if (peer != null) {
        peer.updateUploadedVolume(byteCount);
      }
    } catch (Exception e) {
      if (peer != null) {
        peer.blacklist(e);
      }
      throw e;
    }
  }

}
