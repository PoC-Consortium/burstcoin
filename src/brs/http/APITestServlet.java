package brs.http;

import brs.util.Convert;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class APITestServlet extends HttpServlet {

  private static final Logger logger = LoggerFactory.getLogger(APITestServlet.class);

  private static final String header1 =
      "<!DOCTYPE html>\n"
      + "<html>\n"
      + "<head>\n"
      + "    <meta charset=\"UTF-8\"/>\n"
      + "    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">"
      + "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
      + "    <title>Burst http API</title>\n"
      + "    <link href=\"css/bootstrap.min.css\" rel=\"stylesheet\" type=\"text/css\" />"
      + "    <style type=\"text/css\">\n"
      + "        table {border-collapse: collapse;}\n"
      + "        td {padding: 10px;}\n"
      + "        .result {white-space: pre; font-family: monospace; overflow: auto;}\n"
      + "    </style>\n"
      + "    <script type=\"text/javascript\">\n"
      + "        var apiCalls;\n"
      + "        function performSearch(searchStr) {\n"
      + "            if (searchStr == '') {\n"
      + "              $('.api-call-All').show();\n"
      + "            } else {\n"
      + "              $('.api-call-All').hide();\n"
      + "              $('.topic-link').css('font-weight', 'normal');\n"
      + "              for(var i=0; i<apiCalls.length; i++) {\n"
      + "                var apiCall = apiCalls[i];\n"
      + "                if (new RegExp(searchStr.toLowerCase()).test(apiCall.toLowerCase())) {\n"
      + "                  $('#api-call-' + apiCall).show();\n"
      + "                }\n"
      + "              }\n"
      + "            }\n"
      + "        }\n"
      + "        function submitForm(form) {\n"
      + "            var url = '/burst';\n"
      + "            var params = {};\n"
      + "            for (i = 0; i < form.elements.length; i++) {\n"
      + "                if (form.elements[i].type != 'button' && form.elements[i].value && form.elements[i].value != 'submit') {\n"
      + "                    params[form.elements[i].name] = form.elements[i].value;\n"
      + "                }\n"
      + "            }\n"
      + "            $.ajax({\n"
      + "                url: url,\n"
      + "                type: 'POST',\n"
      + "                data: params\n"
      + "            })\n"
      + "            .done(function(result) {\n"
      + "                var resultStr = JSON.stringify(JSON.parse(result), null, 4);\n"
      + "                form.getElementsByClassName(\"result\")[0].textContent = resultStr;\n"
      + "            })\n"
      + "            .fail(function() {\n"
      + "                alert('API not available, check if Burst Server is running!');\n"
      + "            });\n"
      + "            if ($(form).has('.uri-link').length > 0) {\n"
      + "                  var uri = '/burst?' + jQuery.param(params);\n"
      + "                  var html = '<a href=\"' + uri + '\" target=\"_blank\" style=\"font-size:12px;font-weight:normal;\">Open GET URL</a>';"
      + "                  form.getElementsByClassName(\"uri-link\")[0].innerHTML = html;\n"
      + "            }"
      + "            return false;\n"
      + "        }\n"
      + "    </script>\n"
      + "</head>\n"
      + "<body>\n"
      + "<div class=\"navbar navbar-default\" role=\"navigation\">"
      + "   <div class=\"container\" style=\"min-width: 90%;\">"
      + "       <div class=\"navbar-header\">"
      + "           <a class=\"navbar-brand\" href=\"/test\">Burst http API</a>"
      + "       </div>"
      + "       <div class=\"navbar-collapse collapse\">"
      + "           <ul class=\"nav navbar-nav navbar-right\">"
      + "               <li><input type=\"text\" class=\"form-control\" id=\"search\" "
      + "                    placeholder=\"Search\" style=\"margin-top:8px;\"></li>\n"
      + "               <li><a href=\"https://burstwiki.org/wiki/The_Burst_API\" target=\"_blank\" style=\"margin-left:20px;\">Wiki Docs</a></li>"
      + "               <li><a href=\"/doc/index.html\" target=\"_blank\" style=\"margin-left:20px;\">Javadoc Index</a></li>"
      + "           </ul>"
      + "       </div>"
      + "   </div>"
      + "</div>"
      + "<div class=\"container\" style=\"min-width: 90%;\">"
      + "<div class=\"row\">"
      + "  <div class=\"col-xs-12\" style=\"margin-bottom:15px;\">"
      + "    <div class=\"pull-right\">"
      + "      <a href=\"#\" id=\"navi-show-open\">Show Open</a>"
      + "       | "
      + "      <a href=\"#\" id=\"navi-show-all\" style=\"font-weight:bold;\">Show All</a>"
      + "    </div>"
      + "  </div>"
      + "</div>"
      + "<div class=\"row\" style=\"margin-bottom:15px;\">"
      + "  <div class=\"col-xs-4 col-sm-3 col-md-2\">"
      + "    <ul class=\"nav nav-pills nav-stacked\">";
  private static final String header2 =
      "    </ul>"
      + "  </div>"
      + "  <div  class=\"col-xs-8 col-sm-9 col-md-10\">"
      + "    <div class=\"panel-group\" id=\"accordion\">";

  private static final String footer1 =
      "    </div> "
      + "  </div> "
      + "</div> "
      + "</div> "
      + "<script src=\"js/3rdparty/jquery.min.js\" integrity=\"sha384-tsQFqpEReu7ZLhBV2VZlAu7zcOV+rXbYlF2cqB8txI/8aZajjp4Bqd+V6D5IgvKT\"></script>"
      + "<script src=\"js/3rdparty/bootstrap.min.js\" integrity=\"sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa\" type=\"text/javascript\"></script>"
      + "<script>"
      + "  $(document).ready(function() {"
      + "    apiCalls = [];\n";

  private static final String footer2 =
      "    $(\".collapse-link\").click(function(event) {"
      + "       event.preventDefault();"
      + "    });"
      + "    $('#search').keyup(function(e) {\n"
      + "      if (e.keyCode == 13) {\n"
      + "        performSearch($(this).val());\n"
      + "      }\n"
      + "    });\n"
      + "    $('#navi-show-open').click(function(e) {"
      + "      $('.api-call-All').each(function() {"
      + "        if($(this).find('.panel-collapse.in').length != 0) {"
      + "          $(this).show();"
      + "        } else {"
      + "          $(this).hide();"
      + "        }"
      + "      });"
      + "      $('#navi-show-all').css('font-weight', 'normal');"
      + "      $(this).css('font-weight', 'bold');"
      + "      e.preventDefault();"
      + "    });"
      + "    $('#navi-show-all').click(function(e) {"
      + "      $('.api-call-All').show();"
      + "      $('#navi-show-open').css('font-weight', 'normal');"
      + "      $(this).css('font-weight', 'bold');"
      + "      e.preventDefault();"
      + "    });"
      + "  });"
      + "</script>"
      + "</body>\n"
      + "</html>\n";

  private static final List<String> allRequestTypes = new ArrayList<>(APIServlet.apiRequestHandlers.keySet());
  static {
    Collections.sort(allRequestTypes);
  }

  private static final SortedMap<String, SortedSet<String>> requestTags = buildRequestTags();
  private static SortedMap<String, SortedSet<String>> buildRequestTags () {
    SortedMap<String, SortedSet<String>> r = new TreeMap<>();
    for (Map.Entry<String, APIServlet.APIRequestHandler> entry : APIServlet.apiRequestHandlers.entrySet()) {
      final String requestType = entry.getKey();
      final Set<APITag> apiTags = entry.getValue().getAPITags();
      for (APITag apiTag : apiTags) {
          SortedSet<String> set = r.computeIfAbsent(apiTag.name(), k -> new TreeSet<>());
          set.add(requestType);
      }
    }
    return r;
  }

  private static String buildLinks(HttpServletRequest req) {
    final StringBuilder buf = new StringBuilder();
    final String requestTag = Convert.nullToEmpty(req.getParameter("requestTag"));
    buf.append("<li");
    if (requestTag.equals("")) {
      buf.append(" class=\"active\"");
    }
    buf.append("><a href=\"/test\">All</a></li>");
    for (APITag apiTag : APITag.values()) {
      if (requestTags.get(apiTag.name()) != null) {
        buf.append("<li");
        if (requestTag.equals(apiTag.name())) {
          buf.append(" class=\"active\"");
        }
        buf.append("><a href=\"/test?requestTag=").append(apiTag.name()).append("\">");
        buf.append(apiTag.getDisplayName()).append("</a></li>").append(" ");
      }
    }
    return buf.toString();
  }

  protected void doGet(HttpServletRequest req, HttpServletResponse resp) {

    resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private");
    resp.setHeader("Pragma", "no-cache");
    resp.setDateHeader("Expires", 0);
    resp.setContentType("text/html; charset=UTF-8");

    if (API.allowedBotHosts != null && ! API.allowedBotHosts.toString().contains(req.getRemoteHost())) {
      try {
        resp.sendError(HttpServletResponse.SC_FORBIDDEN);
      }
      catch ( IOException e ) {
        logger.debug("IOException: ", e);
      }
      return;
    }

    try {
      try (PrintWriter writer = resp.getWriter()) {
        writer.print(header1);
        writer.print(buildLinks(req));
        writer.print(header2);
        String requestType = Convert.nullToEmpty(req.getParameter("requestType"));
        APIServlet.APIRequestHandler requestHandler = APIServlet.apiRequestHandlers.get(requestType);
        StringBuilder bufJSCalls = new StringBuilder();
        if (requestHandler != null) {
          writer.print(form(requestType, true, requestHandler.getClass().getName(), requestHandler.getParameters(), requestHandler.requirePost()));
          bufJSCalls.append("apiCalls.push(\"").append(requestType).append("\");\n");
        }
        else {
          String requestTag = Convert.nullToEmpty(req.getParameter("requestTag"));
          Set<String> taggedTypes = requestTags.get(requestTag);
          for (String type : (taggedTypes != null ? taggedTypes : allRequestTypes)) {
            requestHandler = APIServlet.apiRequestHandlers.get(type);
            writer.print(form(type, false, requestHandler.getClass().getName(), APIServlet.apiRequestHandlers.get(type).getParameters(), 
                              APIServlet.apiRequestHandlers.get(type).requirePost()));
            bufJSCalls.append("apiCalls.push(\"").append(type).append("\");\n");
          }
        }
        writer.print(footer1);
        writer.print(bufJSCalls.toString());
        writer.print(footer2);
      }
    }
    catch ( IOException e ) {
      logger.debug("IOException: ", e);
    }
  }

  private static String form(String requestType, boolean singleView, String className, List<String> parameters, boolean requirePost) {
    StringBuilder buf = new StringBuilder();
    buf.append("<div class=\"panel panel-default api-call-All\" ");
    buf.append("id=\"api-call-").append(requestType).append("\">");
    buf.append("<div class=\"panel-heading\">");
    buf.append("<h4 class=\"panel-title\">");
    buf.append("<a data-toggle=\"collapse\" class=\"collapse-link\" data-target=\"#collapse").append(requestType).append("\" href=\"#\">");
    buf.append(requestType);
    buf.append("</a>");
    buf.append("<span style=\"float:right;font-weight:normal;font-size:14px;\">");
    if (!singleView) {
      buf.append("<a href=\"/test?requestType=").append(requestType);
      buf.append("\" target=\"_blank\" style=\"font-weight:normal;font-size:14px;color:#777;\"><span class=\"glyphicon glyphicon-new-window\"></span></a>");
      buf.append(" &nbsp;&nbsp;");
    }
    buf.append("<a style=\"font-weight:normal;font-size:14px;color:#777;\" href=\"/doc/");
    buf.append(className.replace('.','/')).append(".html\" target=\"_blank\">javadoc</a>");
    buf.append("</span>");
    buf.append("</h4>");
    buf.append("</div>");
    buf.append("<div id=\"collapse").append(requestType).append("\" class=\"panel-collapse collapse");
    if (singleView) {
      buf.append(" in");
    }
    buf.append("\">");
    buf.append("<div class=\"panel-body\">");
    buf.append("<form action=\"/burst\" method=\"POST\" onsubmit=\"return submitForm(this);\">");
    buf.append("<input type=\"hidden\" name=\"requestType\" value=\"").append(requestType).append("\"/>");
    buf.append("<div class=\"col-xs-12 col-lg-6\" style=\"width: 40%;\">");
    buf.append("<table class=\"table\">");
    for (String parameter : parameters) {
      buf.append("<tr>");
      buf.append("<td>").append(parameter).append(":</td>");
      buf.append("<td><input type=\"");
      buf.append("secretPhrase".equals(parameter) ? "password" : "text");
      buf.append("\" name=\"").append(parameter).append("\" style=\"width:100%;min-width:200px;\"/></td>");
      buf.append("</tr>");
    }
    buf.append("<tr>");
    buf.append("<td colspan=\"2\"><input type=\"submit\" class=\"btn btn-default\" value=\"submit\"/></td>");
    buf.append("</tr>");
    buf.append("</table>");
    buf.append("</div>");
    buf.append("<div class=\"col-xs-12 col-lg-6\" style=\"min-width: 60%;\">");
    buf.append("<h5 style=\"margin-top:0px;\">");
    if (!requirePost) {
      buf.append("<span style=\"float:right;\" class=\"uri-link\">");
      buf.append("</span>");
    } else {
      buf.append("<span style=\"float:right;font-size:12px;font-weight:normal;\">POST only</span>");
    }
    buf.append("Response</h5>");
    buf.append("<pre class=\"result\">JSON response</pre>");
    buf.append("</div>");
    buf.append("</form>");
    buf.append("</div>");
    buf.append("</div>");
    buf.append("</div>");
    return buf.toString();
  }

}
