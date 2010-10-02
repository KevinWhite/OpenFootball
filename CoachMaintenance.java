import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class CoachMaintenance extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="CoachMaintenance";

   public void init()
      {
      context=getServletContext();
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
      int errorField=0;
      String title="";
      String forNames="";
      String surName="";
      String addressLine1="";
      String addressLine2="";
      String townCity="";
      String countyState="";
      String country="";
      String postZipCode="";
      String telephone="";
      String fax="";
      String email="";
      String userName="";
      String password="";
      String rePassword="";
      String administrator="";
      String titleText="";
      response.setContentType("text/html");
      String payPerPlayText=context.getInitParameter("payperplay");
      boolean payPerPlay=false;
      if("yes".equalsIgnoreCase(payPerPlayText))
        {
        payPerPlay=true;
        }
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
      String action=request.getParameter("action");
      boolean admin=false;
      boolean confirm=false;
      if("true".equals(session.getAttribute("administrator")))
        {
        admin=true;
        titleText="Account Maintenance";
        }
      else
        {
        titleText="Account Creation";
        }
      if("Cancel".equals(action))
         {
         try
           {
           response.sendRedirect((String)session.getAttribute("redirect"));
           }
         catch(IOException error)
           {
           Routines.writeToLog(servletName,"Unable to set redirect : " + error,false,context);
           }
         return;
         }
      if("Create My Account".equals(action))
         {
         title=request.getParameter("title");
         forNames=request.getParameter("forNames");
         surName=request.getParameter("surName");
         addressLine1=request.getParameter("addressLine1");
         addressLine2=request.getParameter("addressLine2");
         townCity=request.getParameter("townCity");
         countyState=request.getParameter("countyState");
         country=request.getParameter("country");
         postZipCode=request.getParameter("postZipCode");
         telephone=request.getParameter("telephone");
         fax=request.getParameter("fax");
         email=request.getParameter("email");
         userName=request.getParameter("userName");
         password=request.getParameter("password");
         rePassword=request.getParameter("rePassword");
         administrator=request.getParameter("administrator");
         errorField=createCoach(admin,
                                title,
                                forNames,
                                surName,
                                addressLine1,
                                addressLine2,
                                townCity,
                                countyState,
                                country,
                                postZipCode,
                                telephone,
                                fax,
                                email,
                                userName,
                                password,
                                rePassword,
                                administrator,
                                session,
                                database,
                                context);
          if(errorField==0)
            {
            confirm=true;
            }
         }
      Routines.WriteHTMLHead(titleText,//title
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
      pool.returnConnection(database);
      if(!admin)
        {
        webPageOutput.println("<CENTER>");
        webPageOutput.println("<IMG SRC=\"../Images/Ticket.gif\"" +
                              " WIDTH='104' HEIGHT='91' ALT='Ticket' ALIGN=\"CENTER\">");
        webPageOutput.println("<IMG SRC=\"../Images/SignUp.jpg\"" +
                              " WIDTH='278' HEIGHT='79' ALT='Sign Up'>");
        webPageOutput.println("<IMG SRC=\"../Images/Ticket.gif\"" +
                              " WIDTH='104' HEIGHT='91' ALT='Ticket' ALIGN=\"CENTER\">");
        webPageOutput.println("</CENTER>");
        Routines.tableStart(false,webPageOutput);
        if(confirm)
          {
          Routines.tableHeader("Welcome, " + forNames + " " + surName,0,webPageOutput);
          Routines.tableDataStart(true,false,false,true,true,0,0,"scoresrow",webPageOutput);
          webPageOutput.println("<B>Your UserName is : " + userName + "</B>");
          webPageOutput.println(Routines.spaceLines(2));
          webPageOutput.println("<B>Your Password is : " + password + "</B>");
          webPageOutput.println(Routines.spaceLines(2));
          webPageOutput.println("Your account has been created, please make a note of your ");
          webPageOutput.println("UserName and Password as you will need them to access ");
          webPageOutput.println("your Front Office on each visit.");
          webPageOutput.println(Routines.spaceLines(2));
          if(payPerPlay)
            {
            webPageOutput.println("You have been given 0 credits to start your account. This will allow you to play ");
            webPageOutput.println("in our free trial leagues, or participate in our test leagues. Get a feel for the game ");
            webPageOutput.println("and if you decide you want to join our pay-per-play leagues, click on the button ");
            webPageOutput.println("in the menu bar to increase your credits (one credit pays for one complete season).");
            webPageOutput.println(Routines.spaceLines(2));
            webPageOutput.println("But were getting ahead of ourselves, lets get you enjoying the freebies.");
            }
          int selectedTeam=Routines.safeParseInt(request.getParameter("team"));
          if(selectedTeam!=0)
            {
            webPageOutput.println(Routines.spaceLines(2));
            webPageOutput.println("<CENTER>");
            webPageOutput.println("<FORM ACTION=\"http://" +
                                  request.getServerName() +
                                  ":" +
                                  request.getServerPort() +
                                  request.getContextPath() +
                                  "/servlet/TeamNews\" METHOD=\"GET\">");
            webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Click here to coach the team you selected\" NAME=\"action\">");
            webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"jsessionid\" VALUE=\"" + session.getId() + "\">");
            webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"league\" VALUE=\"" + Routines.safeParseInt((String)session.getAttribute("league")) + "\">");
            webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"coachNumber\" VALUE=\"" + Routines.safeParseInt((String)session.getAttribute("coachNumber")) + "\">");
            webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"team\" VALUE=\"" + selectedTeam + "\">");
            webPageOutput.println("</FORM>");
            webPageOutput.println(Routines.spaceLines(2));
            webPageOutput.println("OR");
            webPageOutput.println(Routines.spaceLines(2));
            webPageOutput.println("</CENTER>");
            webPageOutput.println("Have a search for another team. This is a simple, five step process :");
            }
          else
            {
            webPageOutput.println("Your first port of call is to get yourself a team. This is a simple, five step process :");
            }
          webPageOutput.println(Routines.spaceLines(2));
          if(payPerPlay)
            {
            webPageOutput.println("(1) Click on any of the Beta or Trial leagues, as listed in the side menu.");
            }
          else
            {
            webPageOutput.println("(1) Click on any league, as listed in the side menu.");
            }
          webPageOutput.println(Routines.spaceLines(1));
          webPageOutput.println("(2) Click on the Standings page for that league, as listed in the top menu.");
          webPageOutput.println(Routines.spaceLines(1));
          webPageOutput.println("(3) Choose a team to coach, available teams have the coach name in italics.");
          webPageOutput.println(Routines.spaceLines(1));
          webPageOutput.println("(4) Click on the team that you wish to coach, to show the Team Information page.");
          webPageOutput.println(Routines.spaceLines(1));
          webPageOutput.println("(5) Click on the \"Coach This Team\" Button.");
          webPageOutput.println(Routines.spaceLines(2));
          webPageOutput.println("You now have a team. The page should show you as the coach of your chosen team ");
          webPageOutput.println("and a MyTeam button will have appeared in the side menu. The MyTeam button is the gateway to ");
          webPageOutput.println("your Front Office, from where you can take the helm of your new team.");
          webPageOutput.println(Routines.spaceLines(2));
          webPageOutput.println("We have taken the liberty of logging you in already for this session.");
          webPageOutput.println(Routines.spaceLines(2));
          webPageOutput.println("So go choose yourself a team and have fun....");
          webPageOutput.println(Routines.clubPitSig());
          Routines.tableDataEnd(false,true,true,webPageOutput);
          }
        else
          {
          Routines.tableHeader("Sign Up Procedure",0,webPageOutput);
          Routines.tableDataStart(true,false,false,true,true,0,0,"scoresrow",webPageOutput);
          if(payPerPlay)
            {
            webPageOutput.println("Please complete the form below, you will then have ");
            webPageOutput.println("a basic account with us and can join our free trial leagues.");
            }
          else
            {
            webPageOutput.println("Please complete the form below, you will then be able ");
            webPageOutput.println("to join any of our leagues.");
            }
          Routines.tableDataEnd(false,true,true,webPageOutput);
          }
        Routines.tableEnd(webPageOutput);
        }
      if(!confirm)
        {
        displayForm(admin,
                    title,
                    forNames,
                    surName,
                    addressLine1,
                    addressLine2,
                    townCity,
                    countyState,
                    country,
                    postZipCode,
                    telephone,
                    fax,
                    email,
                    userName,
                    password,
                    rePassword,
                    administrator,
                    errorField,
                    request,
                    webPageOutput);
        }
      webPageOutput.println(Routines.spaceLines(1));
      Routines.WriteHTMLTail(request,response,webPageOutput);
      }

   private int createCoach(boolean admin,
                           String title,
                           String forNames,
                           String surName,
                           String addressLine1,
                           String addressLine2,
                           String townCity,
                           String countyState,
                           String country,
                           String postZipCode,
                           String telephone,
                           String fax,
                           String email,
                           String userName,
                           String password,
                           String rePassword,
                           String administrator,
                           HttpSession session,
                           Connection database,
                           ServletContext context)
      {
      int coachNumber=0;
      if(!Routines.containsText(surName))
        {
        session.setAttribute("message","Please supply a surname");
        return 1;
        }
      if(!Routines.containsText(userName))
        {
        session.setAttribute("message","Please supply a username");
        return 2;
        }
      if(!Routines.containsText(password))
        {
        session.setAttribute("message","Please supply a password");
        return 3;
        }
      if(!Routines.containsText(rePassword))
        {
        session.setAttribute("message","Please supply a confirmed password");
        return 3;
        }
      if(!password.equals(rePassword))
        {
        session.setAttribute("message","Passwords do not match, please re-enter");
        return 3;
        }
      try
        {
        Statement sql = database.createStatement();
        ResultSet queryResults;
        queryResults=sql.executeQuery("SELECT CoachNumber " +
                                      "FROM coaches " +
                                      "WHERE Username='" + userName + "'");
        if(queryResults.first())
          {
          session.setAttribute("message","UserName already taken, please try another");
          return 2;
          }
        }
      catch(SQLException error)
        {
        session.setAttribute("message",error.getMessage());
        return 2;
        }
      int administratorInt=0;
      if("true".equals(administrator)&&admin)
        {
        administratorInt=1;
        }
      synchronized(this)
        {
        try
          {
          Statement sql = database.createStatement();
          ResultSet queryResults;
          queryResults=sql.executeQuery("SELECT CoachNumber " +
                                        "FROM coaches " +
                                        "ORDER BY CoachNumber DESC");
          coachNumber=0;
          if(queryResults.first())
            {
            coachNumber=queryResults.getInt(1);
            }
          coachNumber++;
          int updated=sql.executeUpdate("INSERT INTO coaches " +
                                        "(CoachNumber,Title,Forenames,Surname," +
                                        "AddressLine1,AddressLine2,TownCity,CountyState," +
                                        "Country,PostZipCode,Telephone,Fax,Email,Username," +
                                        "Password,Administrator,DateTimeStamp " +
                                        ") VALUES (" +
                                        coachNumber + ",'" +
                                        title + "','" +
                                        forNames + "','" +
                                        surName + "','" +
                                        addressLine1 + "','" +
                                        addressLine2 + "','" +
                                        townCity + "','" +
                                        countyState + "','" +
                                        country + "','" +
                                        postZipCode + "','" +
                                        telephone + "','" +
                                        fax + "','" +
                                        email + "','" +
                                        userName + "','" +
                                        password + "'," +
                                        administratorInt + ",'" +
                                        Routines.getDateTime(false) + "'" +
                                        ")");
            }
          catch(SQLException error)
            {
            session.setAttribute("message",error.getMessage());
            }
          }
        boolean[] loginAttemptResults=new boolean[2];
        int league=Routines.safeParseInt((String)session.getAttribute("league"));
        loginAttemptResults=SignIn.login(true,userName,password,"False",league,
                                         session,null,database,context);
        return 0;
      }

   public void displayForm(boolean admin,
                           String title,
                           String forNames,
                           String surName,
                           String addressLine1,
                           String addressLine2,
                           String townCity,
                           String countyState,
                           String country,
                           String postZipCode,
                           String telephone,
                           String fax,
                           String email,
                           String userName,
                           String password,
                           String rePassword,
                           String administrator,
                           int errorField,
                           HttpServletRequest request,
                           PrintWriter webPageOutput)
      {
      HttpSession session=request.getSession();
      int titleWidth=20;
      int inputWidth=80;
      Routines.tableStart(false,webPageOutput);
      Routines.tableDataStart(true,false,true,true,true,0,0,"scoresrow",webPageOutput);
      webPageOutput.println("<FORM ACTION=\"http://" +
                             request.getServerName() +
                             ":" +
                             request.getServerPort() +
                             request.getContextPath() +
                             "/servlet/CoachMaintenance\" METHOD=\"POST\">");
      Routines.messageCheck(true,request,webPageOutput);
      Routines.tableStart(false,webPageOutput);
      Routines.tableDataStart(true,false,true,true,true,titleWidth,0,"scoresrow",webPageOutput);
      webPageOutput.println("Title");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,inputWidth,0,"scoresrow",webPageOutput);
      webPageOutput.println("<SELECT NAME=\"title\">");
      if("Mr".equals(title))
        {
        webPageOutput.println("<OPTION SELECTED VALUE=\"Mr\">Mr");
        }
      else
        {
        webPageOutput.println("<OPTION VALUE=\"Mr\">Mr");
        }
      if("Mrs".equals(title))
        {
        webPageOutput.println("<OPTION SELECTED VALUE=\"Mrs\">Mrs");
        }
      else
        {
        webPageOutput.println("<OPTION VALUE=\"Mrs\">Mrs");
        }
      if("Miss".equals(title))
        {
        webPageOutput.println("<OPTION SELECTED VALUE=\"Miss\">Miss");
        }
      else
        {
        webPageOutput.println("<OPTION VALUE=\"Miss\">Miss");
        }
      if("Ms".equals(title))
        {
        webPageOutput.println("<OPTION SELECTED VALUE=\"Ms\">Ms");
        }
      else
        {
        webPageOutput.println("<OPTION VALUE=\"Ms\">Ms");
        }
      if("Dr".equals(title))
        {
        webPageOutput.println("<OPTION SELECTED VALUE=\"Dr\">Dr");
        }
      else
        {
        webPageOutput.println("<OPTION VALUE=\"Dr\">Dr");
        }
      webPageOutput.println("</SELECT>");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableDataStart(true,false,true,true,true,titleWidth,0,"scoresrow",webPageOutput);
      webPageOutput.println("Forenames");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,inputWidth,0,"scoresrow",webPageOutput);
      webPageOutput.print("<INPUT TYPE=\"TEXT\" NAME=\"forNames\" SIZE=\"20\" MAXLENGTH=\"50\"");
      if(forNames!=""&&forNames!=null)
        {
        webPageOutput.print(" VALUE=\"" + forNames + "\"");
        }
      webPageOutput.println(">");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableDataStart(true,false,true,true,true,titleWidth,0,"scoresrow",webPageOutput);
      webPageOutput.print("<FONT COLOR=\"#FF0000\">");
      webPageOutput.print("*");
      if(errorField==1)
        {
        webPageOutput.println("Surname");
        webPageOutput.println("</FONT>");
        }
      else
        {
        webPageOutput.println("</FONT>");
        webPageOutput.println("Surname");
        }
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,inputWidth,0,"scoresrow",webPageOutput);
      webPageOutput.println("<INPUT TYPE=\"TEXT\" NAME=\"surName\" SIZE=\"20\" MAXLENGTH=\"50\"");
      if(surName!=""&&surName!=null)
        {
        webPageOutput.print(" VALUE=\"" + surName + "\"");
        }
      webPageOutput.println(">");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableDataStart(true,false,true,true,true,titleWidth,0,"scoresrow",webPageOutput);
      webPageOutput.println("Address Line 1");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,inputWidth,0,"scoresrow",webPageOutput);
      webPageOutput.println("<INPUT TYPE=\"TEXT\" NAME=\"addressLine1\" SIZE=\"20\" MAXLENGTH=\"50\"");
      if(addressLine1!=""&&addressLine1!=null)
        {
        webPageOutput.print(" VALUE=\"" + addressLine1 + "\"");
        }
      webPageOutput.println(">");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableDataStart(true,false,true,true,true,titleWidth,0,"scoresrow",webPageOutput);
      webPageOutput.println("Address Line 2");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,inputWidth,0,"scoresrow",webPageOutput);
      webPageOutput.println("<INPUT TYPE=\"TEXT\" NAME=\"addressLine2\" SIZE=\"20\" MAXLENGTH=\"50\"");
      if(addressLine2!=""&&addressLine2!=null)
        {
        webPageOutput.print(" VALUE=\"" + addressLine2 + "\"");
        }
      webPageOutput.println(">");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableDataStart(true,false,true,true,true,titleWidth,0,"scoresrow",webPageOutput);
      webPageOutput.println("Town/City");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,inputWidth,0,"scoresrow",webPageOutput);
      webPageOutput.println("<INPUT TYPE=\"TEXT\" NAME=\"townCity\" SIZE=\"20\" MAXLENGTH=\"50\"");
      if(townCity!=""&&townCity!=null)
        {
        webPageOutput.print(" VALUE=\"" + townCity + "\"");
        }
      webPageOutput.println(">");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableDataStart(true,false,true,true,true,titleWidth,0,"scoresrow",webPageOutput);
      webPageOutput.println("County/State");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,inputWidth,0,"scoresrow",webPageOutput);
      webPageOutput.println("<INPUT TYPE=\"TEXT\" NAME=\"countyState\" SIZE=\"20\" MAXLENGTH=\"50\"");
      if(countyState!=""&&countyState!=null)
        {
        webPageOutput.print(" VALUE=\"" + countyState + "\"");
        }
      webPageOutput.println(">");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableDataStart(true,false,true,true,true,titleWidth,0,"scoresrow",webPageOutput);
      webPageOutput.println("Country");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,inputWidth,0,"scoresrow",webPageOutput);
      webPageOutput.println("<INPUT TYPE=\"TEXT\" NAME=\"country\" SIZE=\"20\" MAXLENGTH=\"50\"");
      if(country!=""&&country!=null)
        {
        webPageOutput.print(" VALUE=\"" + country + "\"");
        }
      webPageOutput.println(">");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableDataStart(true,false,true,true,true,titleWidth,0,"scoresrow",webPageOutput);
      webPageOutput.println("Post/Zip Code");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,inputWidth,0,"scoresrow",webPageOutput);
      webPageOutput.println("<INPUT TYPE=\"TEXT\" NAME=\"postZipCode\" SIZE=\"20\" MAXLENGTH=\"50\"");
      if(postZipCode!=""&&postZipCode!=null)
        {
        webPageOutput.print(" VALUE=\"" + postZipCode + "\"");
        }
      webPageOutput.println(">");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableDataStart(true,false,true,true,true,titleWidth,0,"scoresrow",webPageOutput);
      webPageOutput.println("Telephone");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,inputWidth,0,"scoresrow",webPageOutput);
      webPageOutput.println("<INPUT TYPE=\"TEXT\" NAME=\"telephone\" SIZE=\"20\" MAXLENGTH=\"50\"");
      if(telephone!=""&&telephone!=null)
        {
        webPageOutput.print(" VALUE=\"" + telephone + "\"");
        }
      webPageOutput.println(">");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableDataStart(true,false,true,true,true,titleWidth,0,"scoresrow",webPageOutput);
      webPageOutput.println("Fax");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,inputWidth,0,"scoresrow",webPageOutput);
      webPageOutput.println("<INPUT TYPE=\"TEXT\" NAME=\"fax\" SIZE=\"20\" MAXLENGTH=\"50\"");
      if(fax!=""&&fax!=null)
        {
        webPageOutput.print(" VALUE=\"" + fax + "\"");
        }
      webPageOutput.println(">");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableDataStart(true,false,true,true,true,titleWidth,0,"scoresrow",webPageOutput);
      webPageOutput.println("Email");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,inputWidth,0,"scoresrow",webPageOutput);
      webPageOutput.println("<INPUT TYPE=\"TEXT\" NAME=\"email\" SIZE=\"20\" MAXLENGTH=\"50\"");
      if(email!=""&&email!=null)
        {
        webPageOutput.print(" VALUE=\"" + email + "\"");
        }
      webPageOutput.println(">");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableDataStart(true,false,true,true,true,titleWidth,0,"scoresrow",webPageOutput);
      webPageOutput.print("<FONT COLOR=\"#FF0000\">");
      webPageOutput.print("*");
      if(errorField==2)
        {
        webPageOutput.println("UserName");
        webPageOutput.println("</FONT>");
        }
      else
        {
        webPageOutput.println("</FONT>");
        webPageOutput.println("UserName");
        }
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,inputWidth,0,"scoresrow",webPageOutput);
      webPageOutput.println(" <INPUT TYPE=\"TEXT\" NAME=\"userName\" SIZE=\"20\" MAXLENGTH=\"50\"");
      if(userName!=""&&userName!=null)
        {
        webPageOutput.print(" VALUE=\"" + userName + "\"");
        }
      webPageOutput.println(">");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableDataStart(true,false,true,true,true,titleWidth,0,"scoresrow",webPageOutput);
      webPageOutput.print("<FONT COLOR=\"#FF0000\">");
      webPageOutput.print("*");
      if(errorField==3)
        {
        webPageOutput.println("Password");
        webPageOutput.println("</FONT>");
        }
      else
        {
        webPageOutput.println("</FONT>");
        webPageOutput.println("Password");
        }
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,inputWidth,0,"scoresrow",webPageOutput);
      webPageOutput.println("<INPUT TYPE=\"TEXT\" NAME=\"password\" SIZE=\"20\" MAXLENGTH=\"50\"");
      if(password!=""&&password!=null)
        {
        webPageOutput.print(" VALUE=\"" + password + "\"");
        }
      webPageOutput.println(">");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableDataStart(true,false,true,true,true,titleWidth,0,"scoresrow",webPageOutput);
      webPageOutput.print("<FONT COLOR=\"#FF0000\">");
      webPageOutput.print("*");
      if(errorField==4)
        {
        webPageOutput.println("Confirm Password");
        webPageOutput.println("</FONT>");
        }
      else
        {
        webPageOutput.println("</FONT>");
        webPageOutput.println("Confirm Password");
        }
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,inputWidth,0,"scoresrow",webPageOutput);
      webPageOutput.println("<INPUT TYPE=\"TEXT\" NAME=\"rePassword\" SIZE=\"20\" MAXLENGTH=\"50\"");
      if(rePassword!=""&&rePassword!=null)
        {
        webPageOutput.print(" VALUE=\"" + rePassword + "\"");
        }
      webPageOutput.println(">");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      if(admin)
        {
        Routines.tableDataStart(true,false,true,true,true,titleWidth,0,"scoresrow",webPageOutput);
        webPageOutput.println("Administrator");
        Routines.tableDataEnd(true,false,false,webPageOutput);
        Routines.tableDataStart(true,false,true,false,false,inputWidth,0,"scoresrow",webPageOutput);
        webPageOutput.println("<INPUT TYPE=\"CHECKBOX\" NAME=\"administrator\" VALUE=\"true\"");
        if(administrator!=""&&administrator!=null)
          {
          webPageOutput.print(" CHECKED");
          }
        webPageOutput.println(">");
        Routines.tableDataEnd(true,true,true,webPageOutput);
        }
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      webPageOutput.print("<FONT COLOR=\"#FF0000\">");
      webPageOutput.print("Fields marked with an * are mandatory");
      webPageOutput.println("</FONT>");
      webPageOutput.println(Routines.spaceLines(2));
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Create My Account\" NAME=\"action\">");
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Cancel\" NAME=\"action\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"jsessionid\" VALUE=\"" + session.getId() + "\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"league\" VALUE=\"" + Routines.safeParseInt((String)session.getAttribute("league")) + "\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"team\" VALUE=\"" + Routines.safeParseInt(request.getParameter("team")) + "\">");
      webPageOutput.println("</FORM>");
      Routines.tableDataEnd(true,true,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      }
   }