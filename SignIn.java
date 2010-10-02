import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class SignIn extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="SignIn";

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

   public void doPost(HttpServletRequest request,
                      HttpServletResponse response)
      {
      doGet(request,response);
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
      Connection database=null;
      try
        {
        database=pool.getConnection(servletName);
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to connect to database : " + error,false,context);
        }
      Routines.WriteHTMLHead("Sign In",//title
                             true,//showMenu
                             8,//menuHighLight
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
      boolean[] loginAttemptResults=new boolean[2];
      int league=Routines.safeParseInt((String)session.getAttribute("league"));
      int season=Routines.safeParseInt((String)session.getAttribute("season"));
      int week=Routines.safeParseInt((String)session.getAttribute("week"));
      String redirect=(String)session.getAttribute("redirect");
      String action=request.getParameter("action");
      String userName=null;
      userName=request.getParameter("userName");
      loginAttemptResults[0]=false;
      loginAttemptResults[1]=false;
      if(redirect==null)
        {
        session.setAttribute("redirect",
                             "http://" +
                             request.getServerName() +
                             ":" +
                             request.getServerPort() +
                             request.getContextPath() +
                             "/servlet/Main?jsessionid=" + session.getId());
        }
      if ("Cancel".equals(action))
         {
         try
           {	
           response.sendRedirect((String)session.getAttribute("redirect"));
           }
         catch(IOException error)
           {
		   Routines.writeToLog(servletName,"Unable to redirect : " + error,false,context);	
           }	  
         return;
         }
      if ("Sign In".equals(action))
         {
         userName=request.getParameter("userName");
         String password=request.getParameter("password");
         userName=userName.toLowerCase();
		 password=password.toLowerCase();
         String setCookie=request.getParameter("setCookie");
         int coachNumber=0;
         loginAttemptResults=login(false,userName,password,setCookie,league,session,response,database,context);
         if(loginAttemptResults[0]&&loginAttemptResults[1])
           {
           try
             {
             response.sendRedirect(Routines.getRedirect(request,response,context));
             }
           catch(IOException error)
             {
		     Routines.writeToLog(servletName,"No redirect found : " + error,false,context);	
             }
           return;
           }
         }
      pool.returnConnection(database);
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Enter your Username and Password",0,webPageOutput);
      Routines.tableDataStart(true,false,true,true,true,0,0,"scoresrow",webPageOutput);
      webPageOutput.println("<FORM ACTION=\"http://" +
                             request.getServerName() +
                             ":" +
                             request.getServerPort() +
                             request.getContextPath() +
                             "/servlet/SignIn\" METHOD=\"POST\">");
      Routines.messageCheck(true,request,webPageOutput);
      webPageOutput.println("<IMG SRC=\"../Images/SignIn.gif\"" +
                            " ALIGN=\"LEFT\" WIDTH='143' HEIGHT='104' ALT='Sign In'>");
      webPageOutput.print("User Name<INPUT TYPE=\"TEXT\" NAME=\"userName\" SIZE=\"20\" MAXLENGTH=\"20\"");
      if("null".equals(userName))
        {
        userName=(String)session.getAttribute("userName");
        }
      if(userName!=null)
         {
         webPageOutput.print(" VALUE=\"" + userName + "\"");
         }
      webPageOutput.print(">");
      if ("Sign In".equals(action)&&!loginAttemptResults[0])
         {
         webPageOutput.print("<FONT COLOR=\"#FF0000\">");
         webPageOutput.print("&nbsp*** Invalid User Name *** ");
         webPageOutput.println("</FONT>");
         }
      webPageOutput.println(Routines.spaceLines(2));
      webPageOutput.print("Password&nbsp&nbsp<INPUT TYPE=\"PASSWORD\" NAME=\"password\" SIZE=\"20\" MAXLENGTH=\"20\">");
      if (loginAttemptResults[0]&&!loginAttemptResults[1])
         {
         webPageOutput.print("<FONT COLOR=\"#FF0000\">");
         webPageOutput.print("&nbsp*** Invalid Password *** ");
         webPageOutput.println("</FONT>");
         }
      webPageOutput.println(Routines.spaceLines(2));
      webPageOutput.print("Remember Me<INPUT TYPE=\"CHECKBOX\" NAME=\"setCookie\" VALUE=\"true\">");
      webPageOutput.println(Routines.spaceLines(2));
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Sign In\" NAME=\"action\">");
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Cancel\" NAME=\"action\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"jsessionid\" VALUE=\"" + session.getId() + "\">");
      if (league!=0)
         {
         webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"league\" VALUE=\"" + league + "\">");
         webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"season\" VALUE=\"" + season + "\">");
         webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"week\" VALUE=\"" + week + "\">");
         }
      webPageOutput.println("</FORM>");
      Routines.tableDataEnd(false,true,true,webPageOutput);
      Routines.tableHeader("Remember Me?",0,webPageOutput);
      Routines.tableDataStart(true,false,false,true,true,0,0,"scoresrow",webPageOutput);
      webPageOutput.println("This option allows you to use the site without entering your details " +
                            "on every visit. Your username is stored in your computer and sent to us " +
                            "automatically whenever you visit the site. Therefore this option " +
                            "should only be used on your own personal computer. You will still need to " +
                            "enter a password to access your private information.");
      webPageOutput.println(Routines.spaceLines(2));
      webPageOutput.println("Our \"Remember Me\" option is not set by default, you need to click on it " +
                            "to activate it. Please follow our Policies link (at the bottom of every screen) for more details on our use " +
                            "of this feature (under Section \"Cookies\").");

      Routines.tableDataEnd(false,true,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(6));
      Routines.WriteHTMLTail(request,response,webPageOutput);
      }

   static boolean[] login(boolean autoLogin,
                          String userName,
                          String password,
                          String setCookie,
                          int league,
                          HttpSession session,
                          HttpServletResponse response,
                          Connection database,
	                      ServletContext context)
     {
     boolean[] loginAttemptResults=new boolean[2];
     loginAttemptResults[0]=false;
     loginAttemptResults[1]=false;
     boolean administrator=false;
     int coachNumber=0;
     int credits=0;
     try
       {
       Statement sql=database.createStatement();
       ResultSet queryResults=sql.executeQuery("SELECT CoachNumber,Password,Administrator,Credits " +
                                               "FROM coaches " +
                                               "WHERE Username = '" + userName + "'");
       if(queryResults.first())
	     {
         loginAttemptResults[0]=true;
         coachNumber=queryResults.getInt(1);
         credits=queryResults.getInt(4);
         if(queryResults.getInt(3)==1)
           {
           administrator=true;
           }
         if((queryResults.getString(2)).equals(password))
           {
           loginAttemptResults[1]=true;
           }
         }
       }
     catch(SQLException error)
       {
	   Routines.writeToLog(servletName,"Unable to access coach details : " + error,false,context);		
       }
     if((loginAttemptResults[0]&&loginAttemptResults[1])||
        (autoLogin&&loginAttemptResults[0]))
       {
       session.setAttribute("userName",userName);
       if(loginAttemptResults[1])
         {
         session.setAttribute("password","true");
         }
       else
         {
         session.setAttribute("password","false");
         }
       session.setAttribute("coachNumber",String.valueOf(coachNumber));
       session.setAttribute("administrator",String.valueOf(administrator));
       session.setAttribute("credits",String.valueOf(credits));
       session.setAttribute("teamNumber",String.valueOf(Routines.getTeam(coachNumber,league,database,context)));
       if("true".equals(setCookie))
         {
         Cookie storedUserName = new Cookie("userName",userName);
         storedUserName.setMaxAge(31536000);
         response.addCookie(storedUserName);
         }
       }
    return loginAttemptResults;
    }
   }