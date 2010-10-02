import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class ViewLog extends HttpServlet
   {
   private ConnectionPool pool;
   private static String servletName="ViewLog";
   private ServletContext context;

   public void init()
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
        Routines.writeToLog(servletName,"getWriter error : " + error,false,context);
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
      if(Routines.loginCheck(true,request,response,database,context))
        {
        return;
        }
      String server=context.getInitParameter("server");
      boolean liveSever=false;
      if(server==null)
        {
        server="";
        }
      if(server.equals("live"))
        {
        response.setHeader("Refresh","60");
        }
      Routines.WriteHTMLHead("View System Log",//title
                             false,//showMenu
                             13,//menuHighLight
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
      webPageOutput.println("<CENTER>");
      webPageOutput.println("<IMG SRC=\"../Images/Admin.gif\"" +
                            " WIDTH='125' HEIGHT='115' ALT='Admin'>");
      webPageOutput.println("</CENTER>");
      pool.returnConnection(database);
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("System Log",0,webPageOutput);
      Routines.tableDataStart(true,false,false,true,true,0,0,"scoresrow",webPageOutput);
      boolean firstLine=true;
      int numOfLines=0;
      try
        {
        String file=context.getRealPath("/");
        FileReader logFile=new FileReader(file + "/Data/log.txt");
        BufferedReader logFileBuffer=new BufferedReader(logFile);
        boolean endOfFile=false;
        while(!endOfFile)
             {
             String logFileText=logFileBuffer.readLine();
             if(logFileText==null)
               {
               endOfFile=true;
               }
             else
               {
               if(firstLine)
                 {
                 firstLine=false;
                 }
               else
                 {
                 webPageOutput.println(Routines.spaceLines(1));
                 }
               numOfLines++;
               webPageOutput.println(logFileText);
               }
             }
       logFileBuffer.close();
        }
      catch(IOException error)
        {
        Routines.writeToLog(servletName,"Problem with log file : " + error,false,context);
        }
      Routines.tableDataEnd(false,true,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      if(numOfLines<20)
        {
        webPageOutput.println(Routines.spaceLines(20-numOfLines));
        }
      Routines.WriteHTMLTail(request,response,webPageOutput);
      }
   }