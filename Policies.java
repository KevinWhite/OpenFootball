import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class Policies extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="Policies";

   public void init() throws ServletException
      {
      context = getServletContext();
      synchronized(context)
        {
        pool=(ConnectionPool)context.getAttribute("pool");
        if(pool==null)
          {
          String driverClassName = context.getInitParameter("driverClassName");
          String url = context.getInitParameter("url");
          String userName = context.getInitParameter("username");
          String password = context.getInitParameter("password");
          try
            {
            pool=new ConnectionPool(driverClassName,url,userName,password);
            }
          catch(Exception error)
            {
			Routines.writeToLog(servletName,"Unable to create connection pool : " + error,false,context);
            }
          context.setAttribute("pool",pool);
          }
        }
      }

   public void doGet(HttpServletRequest request,
                     HttpServletResponse response)
      {
      response.setContentType("text/html");
	  PrintWriter webPageOutput=null;
	  try
		{
		webPageOutput=response.getWriter();
		}
	  catch(IOException error)
		{
		Routines.writeToLog(servletName,"Error getting writer : " + error,false,context);
		}
      HttpSession session=request.getSession();
      session.setAttribute("redirect",request.getRequestURL() + "?" + request.getQueryString());
      Connection database=null;
      try
        {
        database=pool.getConnection(servletName);
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to connect to database : " + error,false,context);
        }
      Routines.WriteHTMLHead(null,//title
                             true,//showMenu
                             0,//menuHighLight
                             false,//seasonsMenu
		                     false,//weeksMenu
                             false,//scores
                             false,//standings
                             false,//gameCenter
                             false,//schedules
                             false,//previews
                             false,//teamCenter
		                     false,//draft
                             database,//database
                             request,//request
                             response,//response
                             webPageOutput,//webPageOutput
                             context);//context
      pool.returnConnection(database);
      webPageOutput.println("<CENTER>");
      webPageOutput.println("<IMG SRC=\"../Images/Policies.jpg\"" +
                            " WIDTH='305' HEIGHT='79' ALT='Policies'>");
      webPageOutput.println(Routines.spaceLines(1));
      webPageOutput.println("<IMG SRC=\"../Images/Privacy.gif\"" +
                            " WIDTH='120' HEIGHT='120' ALT='Privacy'>");
      webPageOutput.println("</CENTER>");
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Privacy",0,webPageOutput);
      Routines.tableDataStart(true,false,false,true,true,0,0,"scoresrow",webPageOutput);
      webPageOutput.println("Our policy is simple. We will not share your details with anyone and will not " +
                            "send unsolicited emails to anyone.");
      Routines.tableDataEnd(false,true,true,webPageOutput);
      Routines.tableHeader("Cookies",0,webPageOutput);
      Routines.tableDataStart(true,false,false,true,true,0,0,"scoresrow",webPageOutput);
      webPageOutput.println("Our site will work whether you allow cookies (small pieces of information) " +
                            "to be stored on your computer or choose to reject them. However in terms of adding functionality to our " +
                            "coaches and to improve the efficiency of our site we do make use of cookies if allowed.");
      webPageOutput.println(Routines.spaceLines(2));
      webPageOutput.println("Our cookies are not controversial in their usage. Their existance is finite and if you " +
                            "choose not to return at the end of a season, all our cookies will expire immediately and be removed from your computer.");
      webPageOutput.println(Routines.spaceLines(2));
      webPageOutput.println("We currently use just two cookies. One for your username if you " +
                            "select the \"Remember Me\" option when signing into the site. The other cookie contains a " +
                            "unique session number which is given to you each time you visit. The use of this cookie is to improve the " +
                            "efficiency of the site.");
      Routines.tableDataEnd(false,true,true,webPageOutput);
      Routines.tableHeader("Javascript",0,webPageOutput);
      Routines.tableDataStart(true,false,false,true,true,0,0,"scoresrow",webPageOutput);
      webPageOutput.println("Our site will work whether you allow javascript to be used or if you choose " +
                            "not to allow javascript to run in your browser. Our usage of javascript is not controversial and is " +
                            "entirely for enhancing the experience of using the site. ");
      webPageOutput.println(Routines.spaceLines(2));
      webPageOutput.println("An example of our usage of javascript are in the count down clocks to keep coaches aware " +
                            "of how long is left before the deadline for setting orders expires. Another example is the pop up online manual.");
      webPageOutput.println(Routines.spaceLines(2));
      webPageOutput.println("We do not employ pop up adverts in any way.");
      Routines.tableDataEnd(false,true,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(2));
      Routines.WriteHTMLTail(request,response,webPageOutput);
      }
   }