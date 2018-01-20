package brs.user;

import brs.BurstException;
import brs.util.Subnet;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static brs.http.common.Parameters.REQUEST_TYPE_PARAMETER;
import static brs.http.common.Parameters.USER_PARAMETER;
import static brs.user.JSONResponses.*;
import static brs.Constants.*;

public final class UserServlet extends HttpServlet  {

  private static final Logger logger = LoggerFactory.getLogger(UserServlet.class);

  abstract static class UserRequestHandler {
    abstract JSONStreamAware processRequest(HttpServletRequest request, User user) throws BurstException, IOException;
    boolean requirePost() {
      return false;
    }
  }

  private static final Map<String,UserRequestHandler> userRequestHandlers;

  static {
    final Map<String,UserRequestHandler> map = new HashMap<>();
    map.put("generateAuthorizationToken", GenerateAuthorizationToken.instance);
    map.put("getInitialData", GetInitialData.instance);
    map.put("getNewData", GetNewData.instance);
    map.put("lockAccount", LockAccount.instance);
    map.put("removeActivePeer", RemoveActivePeer.instance);
    map.put("removeBlacklistedPeer", RemoveBlacklistedPeer.instance);
    map.put("removeKnownPeer", RemoveKnownPeer.instance);
    map.put("sendMoney", SendMoney.instance);
    map.put("unlockAccount", UnlockAccount.instance);
    userRequestHandlers = Collections.unmodifiableMap(map);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    try {
      process(req, resp);
    }
    catch ( IOException e ) {
      logger.debug("IOException: ", e);
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    try {
      process(req, resp);
    }
    catch ( IOException e ) {
      logger.debug("IOException: ", e);
    }
  }

  private void process(HttpServletRequest req, HttpServletResponse resp) throws IOException {

    resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private");
    resp.setHeader("Pragma", "no-cache");
    resp.setDateHeader("Expires", 0);

    User user = null;

    try {

      String userPasscode = req.getParameter(USER_PARAMETER);
      if (userPasscode == null) {
        return;
      }
      user = Users.getUser(userPasscode);

      if (Users.allowedUserHosts != null) {
        InetAddress remoteAddress = InetAddress.getByName(req.getRemoteHost());
        boolean allowed = false;
        for (Subnet allowedSubnet: Users.allowedUserHosts)
          {
            if (allowedSubnet.isInNet(remoteAddress))
              {
                allowed = true;
                break;
              }
          }
        if (!allowed)
          {
            user.enqueue(DENY_ACCESS);
            return;
          }
      }

      String requestType = req.getParameter(REQUEST_TYPE_PARAMETER);
      if (requestType == null) {
        user.enqueue(INCORRECT_REQUEST);
        return;
      }

      UserRequestHandler userRequestHandler = userRequestHandlers.get(requestType);
      if (userRequestHandler == null) {
        user.enqueue(INCORRECT_REQUEST);
        return;
      }

      if (userRequestHandler.requirePost() && ! "POST".equals(req.getMethod())) {
        user.enqueue(POST_REQUIRED);
        return;
      }

      JSONStreamAware response = userRequestHandler.processRequest(req, user);
      if (response != null) {
        user.enqueue(response);
      }

    } catch (RuntimeException|BurstException e) {

      logger.info("Error processing GET request", e);
      if (user != null) {
        JSONObject response = new JSONObject();
        response.put(RESPONSE, "showMessage");
        response.put("message", e.toString());
        user.enqueue(response);
      }

    } finally {

      if (user != null) {
        user.processPendingResponses(req, resp);
      }

    }

  }

}
