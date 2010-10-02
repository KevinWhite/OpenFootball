import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class RatePlayers extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="RatePlayers";

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
      int positionNumber=Routines.safeParseInt(request.getParameter("positionNumber"));
      int currentPositionNumber=Routines.safeParseInt(request.getParameter("currentPositionNumber"));
      int leagueNumber=Routines.safeParseInt(request.getParameter("league"));
      int teamNumber=Routines.safeParseInt(request.getParameter("team"));
      int currentPage=Routines.safeParseInt(request.getParameter("currentPage"));
      int currentSkillsButton=Routines.safeParseInt(request.getParameter("currentSkillButton"));
      String pageText=request.getParameter("page");
      String skillsText=request.getParameter("skills");
      boolean setupDraft=false;
      if(currentPositionNumber!=positionNumber)
        {
        currentPage=0;
        currentSkillsButton=1;
        pageText="";
        skillsText="";
        }
      if(currentSkillsButton==0)
        {
        currentSkillsButton=1;
        }
      int page=0;
      if("Overall".equals(skillsText))
        {
        currentSkillsButton=1;
        }
      if("Skills1".equals(skillsText))
        {
        currentSkillsButton=2;
        }
      if("Skills2".equals(skillsText))
        {
        currentSkillsButton=3;
        }
      if("Skills3".equals(skillsText))
        {
        currentSkillsButton=4;
        }
      if("Page1".equals(pageText)||(pageText==null&&currentPage==1))
        {
        page=1;
        }
      if("Page2".equals(pageText)||(pageText==null&&currentPage==2))
        {
        page=2;
        }
      if("Page3".equals(pageText)||(pageText==null&&currentPage==3))
        {
        page=3;
        }
      if("Page4".equals(pageText)||(pageText==null&&currentPage==4))
        {
        page=4;
        }
      if("Page5".equals(pageText)||(pageText==null&&currentPage==5))
        {
        page=5;
        }
      if("Page6".equals(pageText)||(pageText==null&&currentPage==6))
        {
        page=6;
        }
      if("Page7".equals(pageText)||(pageText==null&&currentPage==7))
        {
        page=7;
        }
      if("Page8".equals(pageText)||(pageText==null&&currentPage==8))
        {
        page=8;
        }
      String positionName="";
      String action=request.getParameter("action");
      String[] positions=null;
      int[] positionNumbers=null;
      if(session.isNew()&&
        ("Page1".equals(pageText)||
         "Page2".equals(pageText)||
         "Page3".equals(pageText)||
         "Page4".equals(pageText)||
         "Page5".equals(pageText)||
         "Page6".equals(pageText)||
         "Page7".equals(pageText)||
         "Page8".equals(pageText)))
        {
        session.setAttribute("redirect",
                             "http://" +
                             request.getServerName() +
                             ":" +
                             request.getServerPort() +
                             request.getContextPath() +
                             "/servlet/RatePlayers?jsessionid=" + session.getId() + "&league=" + leagueNumber + "&team=" + teamNumber + "&setupDraft=true");
        }
      else
        {
        session.setAttribute("redirect",request.getRequestURL() + "?" + request.getQueryString());
        }
      Connection database=null;
      try
        {
        database=pool.getConnection(servletName);
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to connect to database : " + error,false,context);	
        }
      if(Routines.loginCheck(false,request,response,database,context))
        {
        return;
        }
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT COUNT(PositionNumber) " +
                                     "FROM positions " +
                                     "WHERE RealPosition=1 " +
                                     "AND Type!=3");
        int numOfPositions=0;
        if(queryResult.first())
          {
          numOfPositions=queryResult.getInt(1);
          }
        numOfPositions++;
        positions=new String[numOfPositions];
        positionNumbers=new int[numOfPositions];
        queryResult=sql.executeQuery("SELECT PositionNumber,PositionName " +
                                     "FROM positions " +
                                     "WHERE RealPosition=1 " +
                                     "AND Type!=3 " +
                                     "ORDER BY Type ASC, Sequence ASC");
        int currentPosition=1;
        positions[0]="All Positions";
        while(queryResult.next())
          {
          positionNumbers[currentPosition]=queryResult.getInt(1);
          positions[currentPosition]=queryResult.getString(2);
          currentPosition++;
          }
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to get Positions : " + error,false,context);	
        }
      Routines.WriteHTMLHead("Rate Players",//title
                             true,//showMenu
                             9,//menuHighLight
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
      webPageOutput.println("<IMG SRC=\"../Images/RatePlayers.jpg\"" +
                            " WIDTH='495' HEIGHT='79' ALT='Sign Staff'>");
      webPageOutput.println(Routines.spaceLines(1));
      webPageOutput.println("<IMG SRC=\"../Images/Boss.gif\"" +
                            " WIDTH='160' HEIGHT='120' ALT='Sign Staff'>");
      webPageOutput.println("</CENTER>");
      boolean[] returnBool=Routines.playerDraft(leagueNumber,teamNumber,session,database);
      boolean lockDown=returnBool[0];
      boolean playerDraft=returnBool[1];
      boolean updated=true;
      if (playerDraft&&
         ("Move Player Up".equals(action)||
         "Move Player Down".equals(action)))
          {
          updated=updateEntry(action,
                              teamNumber,
                              positionNumber,
                              session,
                              request,
                              database);
          }
      if(!playerDraft||lockDown||"Return to MyTeam page".equals(action))
        {
        if(!playerDraft||lockDown)
          {
          session.setAttribute("message","Draft deadline has passed, the draft will commence shortly");
          }
        session.setAttribute("redirect",
                             "http://" +
                             request.getServerName() +
                             ":" +
                             request.getServerPort() +
                             request.getContextPath() +
                             "/servlet/MyTeam?jsessionid=" + session.getId() + "&league=" + leagueNumber + "&team=" + teamNumber);
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
      if((String)session.getAttribute("message")!=null)
        {
        Routines.tableStart(false,webPageOutput);
        Routines.tableHeader("Messages",0,webPageOutput);
        Routines.tableDataStart(true,false,true,true,false,0,0,"scoresrow",webPageOutput);
        Routines.messageCheck(false,request,webPageOutput);
        Routines.tableDataEnd(true,false,true,webPageOutput);
        Routines.tableEnd(webPageOutput);
        webPageOutput.println(Routines.spaceLines(1));
        }
      webPageOutput.println("<FORM ACTION=\"http://" +
                             request.getServerName() +
                             ":" +
                             request.getServerPort() +
                             request.getContextPath() +
                             "/servlet/RatePlayers\" METHOD=\"POST\">");
      webPageOutput.println("<CENTER>");
      boolean disabledLink1=false;
      boolean disabledLink2=false;
      boolean disabledLink3=false;
      boolean disabledLink4=false;
      boolean disabledLink5=false;
      boolean disabledLink6=false;
      boolean disabledLink7=false;
      boolean disabledLink8=false;
      if(page==0)
        {
        page++;
        }
      if(page==1)
        {
        disabledLink1=true;
        }
      if(page==2)
        {
        disabledLink2=true;
        }
      if(page==3)
        {
        disabledLink3=true;
        }
      if(page==4)
        {
        disabledLink4=true;
        }
      if(page==5)
        {
        disabledLink5=true;
        }
      if(page==6)
        {
        disabledLink6=true;
        }
      if(page==7)
        {
        disabledLink7=true;
        }
      if(page==8)
        {
        disabledLink8=true;
        }
      currentPage=page;
      if(disabledLink1)
        {
        webPageOutput.println("Page 1");
        }
      else
        {
        Routines.WriteHTMLLink(request,
                               response,
                               webPageOutput,
                               "RatePlayers",
                               "page=Page1"+
                               "&league=" + leagueNumber +
                               "&team=" + teamNumber +
                               "&setupDraft=" + setupDraft +
                               "&currentPage=" + currentPage +
                               "&currentSkillButton=" + currentSkillsButton +
                               "&positionNumber=" + positionNumber +
                               "&currentPositionNumber=" + positionNumber,
                               "Page 1",
                               "opt",
                               true);
        }
      webPageOutput.println("<B>·</B>");
      if(disabledLink2)
        {
        webPageOutput.println("Page 2");
        }
      else
        {
        Routines.WriteHTMLLink(request,
                               response,
                               webPageOutput,
                               "RatePlayers",
                               "page=Page2"+
                               "&league=" + leagueNumber +
                               "&team=" + teamNumber +
                               "&setupDraft=" + setupDraft +
                               "&currentPage=" + currentPage +
                               "&currentSkillButton=" + currentSkillsButton +
                               "&positionNumber=" + positionNumber +
                               "&currentPositionNumber=" + positionNumber,
                               "Page 2",
                               "opt",
                               true);
        }
      webPageOutput.println("<B>·</B>");
      if(disabledLink3)
        {
        webPageOutput.println("Page 3");
        }
      else
        {
        Routines.WriteHTMLLink(request,
                               response,
                               webPageOutput,
                               "RatePlayers",
                               "page=Page3"+
                               "&league=" + leagueNumber +
                               "&team=" + teamNumber +
                               "&setupDraft=" + setupDraft +
                               "&currentPage=" + currentPage +
                               "&currentSkillButton=" + currentSkillsButton +
                               "&positionNumber=" + positionNumber +
                               "&currentPositionNumber=" + positionNumber,
                               "Page 3",
                               "opt",
                               true);
        }
      webPageOutput.println("<B>·</B>");
      if(disabledLink4)
        {
        webPageOutput.println("Page 4");
        }
      else
        {
        Routines.WriteHTMLLink(request,
                               response,
                               webPageOutput,
                               "RatePlayers",
                               "page=Page4"+
                               "&league=" + leagueNumber +
                               "&team=" + teamNumber +
                               "&setupDraft=" + setupDraft +
                               "&currentPage=" + currentPage +
                               "&currentSkillButton=" + currentSkillsButton +
                               "&positionNumber=" + positionNumber +
                               "&currentPositionNumber=" + positionNumber,
                               "Page 4",
                               "opt",
                               true);
        }
      webPageOutput.println("<B>·</B>");
      if(disabledLink5)
        {
        webPageOutput.println("Page 5");
        }
      else
        {
        Routines.WriteHTMLLink(request,
                               response,
                               webPageOutput,
                               "RatePlayers",
                               "page=Page5"+
                               "&league=" + leagueNumber +
                               "&team=" + teamNumber +
                               "&setupDraft=" + setupDraft +
                               "&currentPage=" + currentPage +
                               "&currentSkillButton=" + currentSkillsButton +
                               "&positionNumber=" + positionNumber +
                               "&currentPositionNumber=" + positionNumber,
                               "Page 5",
                               "opt",
                               true);
        }
      webPageOutput.println("<B>·</B>");
      if(disabledLink6)
        {
        webPageOutput.println("Page 6");
        }
      else
        {
        Routines.WriteHTMLLink(request,
                               response,
                               webPageOutput,
                               "RatePlayers",
                               "page=Page6"+
                               "&league=" + leagueNumber +
                               "&team=" + teamNumber +
                               "&setupDraft=" + setupDraft +
                               "&currentPage=" + currentPage +
                               "&currentSkillButton=" + currentSkillsButton +
                               "&positionNumber=" + positionNumber +
                               "&currentPositionNumber=" + positionNumber,
                               "Page 6",
                               "opt",
                               true);
        }
      webPageOutput.println("<B>·</B>");
      if(disabledLink7)
        {
        webPageOutput.println("Page 7");
        }
      else
        {
        Routines.WriteHTMLLink(request,
                               response,
                               webPageOutput,
                               "RatePlayers",
                               "page=Page7"+
                               "&league=" + leagueNumber +
                               "&team=" + teamNumber +
                               "&setupDraft=" + setupDraft +
                               "&currentPage=" + currentPage +
                               "&currentSkillButton=" + currentSkillsButton +
                               "&positionNumber=" + positionNumber +
                               "&currentPositionNumber=" + positionNumber,
                               "Page 7",
                               "opt",
                               true);
        }
      if(disabledLink8)
        {
        webPageOutput.println("Page 8");
        }
      else
        {
        Routines.WriteHTMLLink(request,
                               response,
                               webPageOutput,
                               "RatePlayers",
                               "page=Page8"+
                               "&league=" + leagueNumber +
                               "&team=" + teamNumber +
                               "&setupDraft=" + setupDraft +
                               "&currentPage=" + currentPage +
                               "&currentSkillButton=" + currentSkillsButton +
                               "&positionNumber=" + positionNumber +
                               "&currentPositionNumber=" + positionNumber,
                               "Page 8",
                               "opt",
                               true);
        }
      webPageOutput.println("</CENTER>");
      Routines.tableStart(false,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      webPageOutput.println("<SELECT NAME=\"positionNumber\">");
      String selected="";
      int startingPosition=0;
      for(int currentPosition=startingPosition;currentPosition<positions.length;currentPosition++)
         {
         if(positionNumbers[currentPosition]==positionNumber)
           {
           selected=" SELECTED";
           positionName=positions[currentPosition];
           positionNumber=positionNumbers[currentPosition];
           }
         else
           {
           selected="";
           }
         webPageOutput.println(" <OPTION" + selected + " VALUE=\"" + positionNumbers[currentPosition] + "\">" + positions[currentPosition]);
         }
      webPageOutput.println("</SELECT>");
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" NAME=\"action\" VALUE=\"View\">");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      viewScreen(page,
                 leagueNumber,
                 teamNumber,
                 positionNumber,
                 currentPage,
                 currentSkillsButton,
                 positionName,
                 updated,
                 setupDraft,
                 session,
                 database,
                 request,
                 response,
                 webPageOutput);
      pool.returnConnection(database);
      Routines.WriteHTMLTail(request,response,webPageOutput);
      }

   private void viewScreen(int page,
                           int leagueNumber,
                           int teamNumber,
                           int positionNumber,
                           int currentPage,
                           int currentSkillButton,
                           String positionName,
                           boolean updated,
                           boolean setupDraft,
                           HttpSession session,
                           Connection database,
                           HttpServletRequest request,
                           HttpServletResponse response,
                           PrintWriter webPageOutput)
      {
      String[] titleHeader=Routines.getTitleHeaders(positionNumber,currentSkillButton,context);
      Routines.myTableStart(false,webPageOutput);
      Routines.myTableHeader("Ratings for " + positionName,10,webPageOutput);
      int numOfSkillButtons=Routines.getSkillsButtons(positionNumber,context);
      if(currentSkillButton>numOfSkillButtons)
        {
        currentSkillButton=0;
        }
      if(numOfSkillButtons>0)
        {
        if(positionNumber==0)
          {
          Routines.tableDataStart(true,false,false,true,false,0,4,"bg1",webPageOutput);
          }
        else
          {
          Routines.tableDataStart(true,false,false,true,false,0,3,"bg1",webPageOutput);
          }
        boolean disabledLink1=false;
        boolean disabledLink2=false;
        boolean disabledLink3=false;
        boolean disabledLink4=false;
        if(currentSkillButton==1||currentSkillButton==0)
          {
          disabledLink1=true;
          }
        if(currentSkillButton==2)
          {
          disabledLink2=true;
          }
        if(currentSkillButton==3)
          {
          disabledLink3=true;
          }
        if(currentSkillButton==4)
          {
          disabledLink4=true;
          }
        switch(numOfSkillButtons)
              {
              case 1:
                 Routines.tableDataStart(true,true,false,false,false,0,1,"bg1",webPageOutput);
                 webPageOutput.println("Overall");
                 Routines.tableDataEnd(false,false,false,webPageOutput);
                 Routines.tableDataStart(true,true,false,false,false,0,5,"bg1",webPageOutput);
                 break;
              case 2:
                 Routines.tableDataStart(true,true,false,false,false,0,1,"bg1",webPageOutput);
                 if(disabledLink1)
                   {
                   webPageOutput.println("Overall");
                   }
                 else
                   {
                   Routines.WriteHTMLLink(request,
                                          response,
                                          webPageOutput,
                                          "RatePlayers",
                                          "skills=Overall"+
                                          "&league=" + leagueNumber +
                                          "&team=" + teamNumber +
                                          "&setupDraft=" + setupDraft +
                                          "&currentPage=" + currentPage +
                                          "&currentSkillButton=" + currentSkillButton +
                                          "&positionNumber=" + positionNumber +
                                          "&currentPositionNumber=" + positionNumber,
                                          "Overall",
                                          "bg1",
                                          true);
                    }
                 Routines.tableDataEnd(false,false,false,webPageOutput);
                 Routines.tableDataStart(true,true,false,false,false,0,1,"bg1",webPageOutput);
                 if(disabledLink2)
                   {
                   webPageOutput.println("Skills1");
                   }
                 else
                   {
                   Routines.WriteHTMLLink(request,
                                          response,
                                          webPageOutput,
                                          "RatePlayers",
                                          "skills=Skills1"+
                                          "&league=" + leagueNumber +
                                          "&team=" + teamNumber +
                                          "&setupDraft=" + setupDraft +
                                          "&currentPage=" + currentPage +
                                          "&currentSkillButton=" + currentSkillButton +
                                          "&positionNumber=" + positionNumber +
                                          "&currentPositionNumber=" + positionNumber,
                                          "Skills1",
                                          "bg1",
                                          true);
                    }
                 Routines.tableDataEnd(false,false,false,webPageOutput);
                 Routines.tableDataStart(true,true,false,false,false,0,3,"bg1",webPageOutput);
                 break;
              case 3:
                 Routines.tableDataStart(true,true,false,false,false,0,1,"bg1",webPageOutput);
                 if(disabledLink1)
                   {
                   webPageOutput.println("Overall");
                   }
                 else
                   {
                   Routines.WriteHTMLLink(request,
                                          response,
                                          webPageOutput,
                                          "RatePlayers",
                                          "skills=Overall"+
                                          "&league=" + leagueNumber +
                                          "&team=" + teamNumber +
                                          "&setupDraft=" + setupDraft +
                                          "&currentPage=" + currentPage +
                                          "&currentSkillButton=" + currentSkillButton +
                                          "&positionNumber=" + positionNumber +
                                          "&currentPositionNumber=" + positionNumber,
                                          "Overall",
                                          "bg1",
                                          true);
                    }
                 Routines.tableDataEnd(false,false,false,webPageOutput);
                 Routines.tableDataStart(true,true,false,false,false,0,1,"bg1",webPageOutput);
                 if(disabledLink2)
                   {
                   webPageOutput.println("Skills1");
                   }
                 else
                   {
                   Routines.WriteHTMLLink(request,
                                          response,
                                          webPageOutput,
                                          "RatePlayers",
                                          "skills=Skills1"+
                                          "&league=" + leagueNumber +
                                          "&team=" + teamNumber +
                                          "&setupDraft=" + setupDraft +
                                          "&currentPage=" + currentPage +
                                          "&currentSkillButton=" + currentSkillButton +
                                          "&positionNumber=" + positionNumber +
                                          "&currentPositionNumber=" + positionNumber,
                                          "Skills1",
                                          "bg1",
                                          true);
                    }
                 Routines.tableDataEnd(false,false,false,webPageOutput);
                 Routines.tableDataStart(true,true,false,false,false,0,1,"bg1",webPageOutput);
                 if(disabledLink3)
                   {
                   webPageOutput.println("Skills2");
                   }
                 else
                   {
                   Routines.WriteHTMLLink(request,
                                          response,
                                          webPageOutput,
                                          "RatePlayers",
                                          "skills=Skills2"+
                                          "&league=" + leagueNumber +
                                          "&team=" + teamNumber +
                                          "&setupDraft=" + setupDraft +
                                          "&currentPage=" + currentPage +
                                          "&currentSkillButton=" + currentSkillButton +
                                          "&positionNumber=" + positionNumber +
                                          "&currentPositionNumber=" + positionNumber,
                                          "Skills2",
                                          "bg1",
                                          true);
                    }
                 Routines.tableDataEnd(false,false,false,webPageOutput);
                 Routines.tableDataStart(true,true,false,false,false,0,2,"bg1",webPageOutput);
                 break;
              case 4:
                 Routines.tableDataStart(true,true,false,false,false,0,1,"bg1",webPageOutput);
                 if(disabledLink1)
                   {
                   webPageOutput.println("Overall");
                   }
                 else
                   {
                   Routines.WriteHTMLLink(request,
                                          response,
                                          webPageOutput,
                                          "RatePlayers",
                                          "skills=Overall"+
                                          "&league=" + leagueNumber +
                                          "&team=" + teamNumber +
                                          "&setupDraft=" + setupDraft +
                                          "&currentPage=" + currentPage +
                                          "&currentSkillButton=" + currentSkillButton +
                                          "&positionNumber=" + positionNumber +
                                          "&currentPositionNumber=" + positionNumber,
                                          "Overall",
                                          "bg1",
                                          true);
                    }
                 Routines.tableDataEnd(false,false,false,webPageOutput);
                 Routines.tableDataStart(true,true,false,false,false,0,1,"bg1",webPageOutput);
                 if(disabledLink2)
                   {
                   webPageOutput.println("Skills1");
                   }
                 else
                   {
                   Routines.WriteHTMLLink(request,
                                          response,
                                          webPageOutput,
                                          "RatePlayers",
                                          "skills=Skills1"+
                                          "&league=" + leagueNumber +
                                          "&team=" + teamNumber +
                                          "&setupDraft=" + setupDraft +
                                          "&currentPage=" + currentPage +
                                          "&currentSkillButton=" + currentSkillButton +
                                          "&positionNumber=" + positionNumber +
                                          "&currentPositionNumber=" + positionNumber,
                                          "Skills1",
                                          "bg1",
                                          true);
                    }
                 Routines.tableDataEnd(false,false,false,webPageOutput);
                 Routines.tableDataStart(true,true,false,false,false,0,1,"bg1",webPageOutput);
                 if(disabledLink3)
                   {
                   webPageOutput.println("Skills2");
                   }
                 else
                   {
                   Routines.WriteHTMLLink(request,
                                          response,
                                          webPageOutput,
                                          "RatePlayers",
                                          "skills=Skills2"+
                                          "&league=" + leagueNumber +
                                          "&team=" + teamNumber +
                                          "&setupDraft=" + setupDraft +
                                          "&currentPage=" + currentPage +
                                          "&currentSkillButton=" + currentSkillButton +
                                          "&positionNumber=" + positionNumber +
                                          "&currentPositionNumber=" + positionNumber,
                                          "Skills2",
                                          "bg1",
                                          true);
                    }
                 Routines.tableDataEnd(false,false,false,webPageOutput);
                 Routines.tableDataStart(true,true,false,false,false,0,1,"bg1",webPageOutput);
                 if(disabledLink4)
                   {
                   webPageOutput.println("Skills3");
                   }
                 else
                   {
                   Routines.WriteHTMLLink(request,
                                          response,
                                          webPageOutput,
                                          "RatePlayers",
                                          "skills=Skills3"+
                                          "&league=" + leagueNumber +
                                          "&team=" + teamNumber +
                                          "&setupDraft=" + setupDraft +
                                          "&currentPage=" + currentPage +
                                          "&currentSkillButton=" + currentSkillButton +
                                          "&positionNumber=" + positionNumber +
                                          "&currentPositionNumber=" + positionNumber,
                                          "Skills3",
                                          "bg1",
                                          true);
                    }
                 Routines.tableDataEnd(false,false,false,webPageOutput);
                 Routines.tableDataStart(true,true,false,false,false,0,1,"bg1",webPageOutput);
                 break;
              default:
			     Routines.writeToLog(servletName,"Unexpected number of buttons : " + numOfSkillButtons,false,context); 
	      }
        Routines.tableDataEnd(false,false,true,webPageOutput);
        }
      webPageOutput.println("<TR ALIGN=\"left\" CLASS=\"bg1\">");
      webPageOutput.println("<TD ALIGN='center'>No</TD>");
      webPageOutput.println("<TD ALIGN='center'>Sel</TD>");
      webPageOutput.println("<TD>Name</TD>");
      if(positionNumber==0)
        {
        webPageOutput.println("<TD ALIGN='center'>Pos</TD>");
        }
      webPageOutput.println("<TD ALIGN='right'>" + titleHeader[0] + "</TD>");
      webPageOutput.println("<TD ALIGN='right'>" + titleHeader[1] + "</TD>");
      webPageOutput.println("<TD ALIGN='right'>" + titleHeader[2] + "</TD>");
      webPageOutput.println("<TD ALIGN='right'>" + titleHeader[3] + "</TD>");
      webPageOutput.println("<TD ALIGN='right'>" + titleHeader[4] + "</TD>");
      webPageOutput.println("</TR>");
      boolean playersFound=false;
      int minPlayers=((page-1)*25);
      int maxPlayers=(page*24)+(page-1);
      int currentPlayer=0;
      int rate=1;
      try
        {
        int[] positionSkills=Routines.getNumOfSkills(database,context);
        Statement sql=database.createStatement();
        ResultSet queryResult;
        if(positionNumber==0)
          {
          queryResult=sql.executeQuery("SELECT draftratings.PlayerNumber," +
                                       "Surname,Forname," +
                                       "Intelligence,Ego,Attitude,(Potential*10),(BurnRate*10)," +
                                       "Skill1,Skill2,Skill3,Skill4,Skill5," +
                                       "Skill6,Skill7,Skill8,Skill9,Skill10," +
                                       "Skill11,Skill12,Skill13,Skill14,Skill15," +
                                       "Skill16,Skill17,Skill18,Skill19,Skill20,PositionCode,DraftRatingNumber,players.PositionNumber " +
                                       "FROM draftratings,players,colleges,positions " +
                                       "WHERE draftratings.TeamNumber=" + teamNumber + " " +
                                       "AND draftratings.PlayerNumber=players.PlayerNumber " +
                                       "AND colleges.CollegeNumber=players.CollegeNumber " +
                                       "AND players.PositionNumber=positions.PositionNumber " +
                                       "ORDER BY OverallRating ASC");
          }
        else
          {
          queryResult=sql.executeQuery("SELECT draftratings.PlayerNumber," +
                                       "Surname,Forname," +
                                       "Intelligence,Ego,Attitude,(Potential*10),(BurnRate*10)," +
                                       "Skill1,Skill2,Skill3,Skill4,Skill5," +
                                       "Skill6,Skill7,Skill8,Skill9,Skill10," +
                                       "Skill11,Skill12,Skill13,Skill14,Skill15," +
                                       "Skill16,Skill17,Skill18,Skill19,Skill20,PositionCode,DraftRatingNumber,players.PositionNumber " +
                                       "FROM draftratings,players,colleges,positions " +
                                       "WHERE draftratings.TeamNumber=" + teamNumber + " " +
                                       "AND draftratings.PlayerNumber=players.PlayerNumber " +
                                       "AND players.PositionNumber=" + positionNumber + " " +
                                       "AND colleges.CollegeNumber=players.CollegeNumber " +
                                       "AND players.PositionNumber=positions.PositionNumber " +
                                       "ORDER BY PositionRating ASC");
          }
        while(queryResult.next()&&currentPlayer<=maxPlayers)
             {
             if(currentPlayer>=minPlayers)
               {
               if(!playersFound)
                 {
                 playersFound=true;
                 }
               int playerNumber=queryResult.getInt(1);
               String playerName=queryResult.getString(2) + "," + queryResult.getString(3);
               int skills[]=new int[25];
               for(int currentSkill=0;currentSkill<skills.length;currentSkill++)
                  {
                  skills[currentSkill]=queryResult.getInt(4+currentSkill);
                  }
               String positionCode=queryResult.getString(29);
               int draftRatingNumber=queryResult.getInt(30);
               int tempPositionNumber=queryResult.getInt(31);
               int[] displaySkills=Routines.getSkills(skills,positionNumber,positionSkills[tempPositionNumber],currentSkillButton,context);
               Routines.tableDataStart(false,false,false,true,false,5,0,"scoresrow",webPageOutput);
               webPageOutput.print(rate);
               Routines.tableDataEnd(false,false,false,webPageOutput);
               Routines.tableDataStart(false,true,false,false,false,5,0,"scoresrow",webPageOutput);
               boolean selected=false;
               String param="";
               if(!updated)
                 {
                 param=request.getParameter(String.valueOf(draftRatingNumber));
                 if("true".equals(param))
                   {
                   selected=true;
                   }
                 }
               webPageOutput.print("<INPUT TYPE=\"CHECKBOX\" NAME=\"" + draftRatingNumber  + "\" VALUE=\"true\"");
               if(selected)
                 {
                 webPageOutput.print(" CHECKED");
                 }
               webPageOutput.println(">");
               Routines.tableDataEnd(false,false,false,webPageOutput);
               if(positionNumber==0)
                 {
                 Routines.tableDataStart(true,false,false,false,false,25,0,"scoresrow",webPageOutput);
                 }
               else
                 {
                 Routines.tableDataStart(true,false,false,false,false,30,0,"scoresrow",webPageOutput);
                 }
               Routines.WriteHTMLLink(request,
                                      response,
                                      webPageOutput,
                                      "wfafl",
                                      "action=viewPlayer" +
                                      "&value=" +
                                      playerNumber,
                                      playerName,
                                      null,
                                      true);
               Routines.tableDataEnd(false,false,false,webPageOutput);
               if(positionNumber==0)
                 {
                 Routines.tableDataStart(true,false,false,false,false,5,0,"scoresrow",webPageOutput);
                 webPageOutput.println(positionCode);
                 Routines.tableDataEnd(false,false,false,webPageOutput);
                 }
               Routines.tableDataStart(false,false,false,false,false,12,0,"scoresrow",webPageOutput);
               webPageOutput.println("<FONT CLASS=\"rate1\">");
               if(displaySkills[0]!=-1)
                 {
                 webPageOutput.println(Routines.skillsDescription((displaySkills[0]+5)/10));
                 }
               webPageOutput.println("</FONT>");
               Routines.tableDataEnd(false,false,false,webPageOutput);
               Routines.tableDataStart(false,false,false,false,false,12,0,"scoresrow",webPageOutput);
               webPageOutput.println("<FONT CLASS=\"rate1\">");
               if(displaySkills[1]!=-1)
                 {
                 webPageOutput.println(Routines.skillsDescription((displaySkills[1]+5)/10));
                 }
               webPageOutput.println("</FONT>");
               Routines.tableDataEnd(false,false,false,webPageOutput);
               Routines.tableDataStart(false,false,false,false,false,12,0,"scoresrow",webPageOutput);
               webPageOutput.println("<FONT CLASS=\"rate1\">");
               if(displaySkills[2]!=-1)
                 {
                 webPageOutput.println(Routines.skillsDescription((displaySkills[2]+5)/10));
                 }
               webPageOutput.println("</FONT>");
               Routines.tableDataEnd(false,false,false,webPageOutput);
               Routines.tableDataStart(false,false,false,false,false,12,0,"scoresrow",webPageOutput);
               webPageOutput.println("<FONT CLASS=\"rate1\">");
               if(displaySkills[3]!=-1)
                 {
                 webPageOutput.println(Routines.skillsDescription((displaySkills[3]+5)/10));
                 }
               webPageOutput.println("</FONT>");
               Routines.tableDataEnd(false,false,false,webPageOutput);
               Routines.tableDataStart(false,false,false,false,false,12,0,"scoresrow",webPageOutput);
               webPageOutput.println("<FONT CLASS=\"rate1\">");
               if(displaySkills[4]!=-1)
                 {
                 webPageOutput.println(Routines.skillsDescription((displaySkills[4]+5)/10));
                 }
               webPageOutput.println("</FONT>");
               Routines.tableDataEnd(false,false,true,webPageOutput);
               }
             currentPlayer++;
             rate++;
             }
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to retrieve players : " + error,false,context);	
        }
      if(!playersFound)
        {
        Routines.tableDataStart(true,true,false,true,false,0,10,"bg1",webPageOutput);
        webPageOutput.println("No Players found.");
        Routines.tableDataEnd(false,false,true,webPageOutput);
        }
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Actions",0,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      if(playersFound)
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Move Player Up\" NAME=\"action\">");
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Move Player Down\" NAME=\"action\">");
        webPageOutput.println(Routines.spaceLines(1));
        }
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Return to MyTeam page\" NAME=\"action\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"jsessionid\" VALUE=\"" + session.getId() + "\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"league\" VALUE=\"" + leagueNumber + "\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"team\" VALUE=\"" + teamNumber + "\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"setupDraft\" VALUE=\"" + setupDraft + "\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"currentPage\" VALUE=\"" + currentPage + "\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"currentSkillButton\" VALUE=\"" + currentSkillButton + "\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"currentPositionNumber\" VALUE=\"" + positionNumber + "\">");
      webPageOutput.println("</FORM>");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      }

   private synchronized boolean updateEntry(String action,
                                            int teamNumber,
                                            int positionNumber,
                                            HttpSession session,
                                            HttpServletRequest request,
                                            Connection database)
      {
      boolean success=false;
      int sequence=0;
      if(positionNumber==0)
        {
        try
          {
          // Get Latest SequenceNumber.
          Statement sql=database.createStatement();
          ResultSet queryResult;
          queryResult=sql.executeQuery("SELECT PositionRating " +
                                       "FROM draftratings " +
                                       "WHERE TeamNumber=" + teamNumber + " " +
                                       "ORDER BY OverallRating DESC");
          if(queryResult.first())
            {
            sequence=queryResult.getInt(1);
            }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to retrieve draftratings : " + error,false,context);		
          }
        }
      else
        {
        try
          {
          // Get Latest SequenceNumber.
          Statement sql=database.createStatement();
          ResultSet queryResult;
          queryResult=sql.executeQuery("SELECT PositionRating " +
                                       "FROM draftratings,players " +
                                       "WHERE draftratings.PlayerNumber=players.PlayerNumber " +
                                       "AND PositionNumber=" + positionNumber + " " +
                                       "AND draftratings.TeamNumber=" + teamNumber + " " +
                                       "ORDER BY PositionRating DESC");
          if(queryResult.first())
            {
            sequence=queryResult.getInt(1);
            }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to retrieve draftratings : " + error,false,context);		
          }
        }
      if("Move Player Up".equals(action))
        {
        boolean moveRequested=false;
        try
          {
          int updates=0;
          Statement sql1=database.createStatement();
          Statement sql2=database.createStatement();
          Statement sql3=database.createStatement();
          ResultSet queryResult1;
          ResultSet queryResult2;
          ResultSet queryResult3;
          if(positionNumber==0)
            {
            queryResult1=sql1.executeQuery("SELECT DraftRatingNumber,PositionNumber " +
                                           "FROM draftratings,players " +
                                           "WHERE draftratings.TeamNumber=" + teamNumber + " " +
                                           "AND draftratings.PlayerNumber=players.PlayerNumber " +
                                           "ORDER BY OverallRating ASC");
            }
          else
            {
            queryResult1=sql1.executeQuery("SELECT DraftRatingNumber,PositionNumber " +
                                           "FROM draftratings,players " +
                                           "WHERE draftratings.PlayerNumber=players.PlayerNumber " +
                                           "AND players.PositionNumber=" + positionNumber + " " +
                                           "AND draftratings.TeamNumber=" + teamNumber + " " +
                                           "ORDER BY PositionRating ASC");
            }
          while(queryResult1.next())
               {
               int draftRatingNumber=queryResult1.getInt(1);
               int oldDraftRatingNumber=0;
               int currentPositionNumber=queryResult1.getInt(2);
               int currentSequence=0;
               if("true".equals(request.getParameter(String.valueOf(draftRatingNumber))))
                 {
                 if(!moveRequested)
                   {
                   moveRequested=true;
                   }
                 if(positionNumber==0)
                   {
                   queryResult2=sql2.executeQuery("SELECT OverallRating,Forname,Surname,PositionName,OverallRating " +
                                                  "FROM draftratings,positions,players " +
                                                  "WHERE players.PositionNumber=positions.PositionNumber " +
                                                  "AND draftratings.PlayerNumber=players.PlayerNumber " +
                                                  "AND DraftRatingNumber=" + draftRatingNumber);
                   }
                 else
                   {
                   queryResult2=sql2.executeQuery("SELECT PositionRating,Forname,Surname,PositionName,OverallRating " +
                                                  "FROM draftratings,positions,players " +
                                                  "WHERE players.PositionNumber=positions.PositionNumber " +
                                                  "AND draftratings.PlayerNumber=players.PlayerNumber " +
                                                  "AND DraftRatingNumber=" + draftRatingNumber);
                   }
                 queryResult2.first();
                 currentSequence=queryResult2.getInt(1);
                 int oldOverallRating=queryResult2.getInt(5);
                 int newOverallRating=0;
                 if(currentSequence==1)
                   {
                   session.setAttribute("message",queryResult2.getString(2) + " " + queryResult2.getString(3) + " is already at the top of the " + queryResult2.getString(4) + " list");
                   return false;
                   }
                 int tempPositionNumber=0;
                 if(positionNumber==0)
                   {
                   queryResult3=sql3.executeQuery("SELECT PositionNumber " +
                                                  "FROM draftratings,players " +
                                                  "WHERE draftratings.TeamNumber=" + teamNumber + " " +
                                                  "AND draftratings.PlayerNumber=players.PlayerNumber " +
                                                  "AND OverallRating=" + (currentSequence-1));
                   queryResult3.first();
                   tempPositionNumber=queryResult3.getInt(1);
                   if(currentPositionNumber==tempPositionNumber)
                     {
                     updates=sql1.executeUpdate("UPDATE draftratings " +
                                                "SET OverallRating=(OverallRating+1),PositionRating=(PositionRating+1),DateTimeStamp='" +
                                                Routines.getDateTime(false) + "' " +
                                                "WHERE TeamNumber=" + teamNumber + " " +
                                                "AND OverallRating=" + (currentSequence-1));
                     }
                   else
                     {
                     updates=sql1.executeUpdate("UPDATE draftratings " +
                                                "SET OverallRating=(OverallRating+1),DateTimeStamp='" +
                                                Routines.getDateTime(false) + "' " +
                                                "WHERE TeamNumber=" + teamNumber + " " +
                                                "AND OverallRating=" + (currentSequence-1));
                     }
                   }
                 else
                   {
                   queryResult2=sql2.executeQuery("SELECT DraftRatingNumber,OverallRating " +
                                                  "FROM draftratings,players " +
                                                  "WHERE draftratings.PlayerNumber=players.PlayerNumber " +
                                                  "AND PositionNumber=" + positionNumber + " " +
                                                  "AND draftratings.TeamNumber=" + teamNumber + " " +
                                                  "AND PositionRating=" + (currentSequence-1));
                   queryResult2.first();
                   oldDraftRatingNumber=queryResult2.getInt(1);
                   newOverallRating=queryResult2.getInt(2);
                   updates=sql1.executeUpdate("UPDATE draftratings " +
                                              "SET PositionRating=(PositionRating+1),DateTimeStamp='" +
                                              Routines.getDateTime(false) + "' " +
                                              "WHERE DraftRatingNumber=" + oldDraftRatingNumber);
                   }
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Draftrating not moved (prior), reason unknown",false,context);	
                   }
                 if(positionNumber==0)
                   {
                   updates=sql1.executeUpdate("UPDATE draftratings " +
                                              "SET OverallRating=(OverallRating-1),DateTimeStamp='" +
                                              Routines.getDateTime(false)  + "' " +
                                              "WHERE DraftRatingNumber=" + draftRatingNumber);
                   if(currentPositionNumber==tempPositionNumber)
                     {
                     updates=sql1.executeUpdate("UPDATE draftratings " +
                                                "SET PositionRating=(PositionRating-1),DateTimeStamp='" +
                                                Routines.getDateTime(false) + "' " +
                                                "WHERE DraftRatingNumber=" + draftRatingNumber);
                     }
                   }
                 else
                   {
                   updates=sql1.executeUpdate("UPDATE draftratings " +
                                              "SET PositionRating=(PositionRating-1),DateTimeStamp='" +
                                              Routines.getDateTime(false)  + "'," +
                                              "OverallRating=" + newOverallRating + " " +
                                              "WHERE DraftRatingNumber=" + draftRatingNumber);
                  sql1.executeUpdate("UPDATE draftratings " +
                                     "SET OverallRating=(OverallRating+1),DateTimeStamp='" +
                                     Routines.getDateTime(false)  + "' " +
                                     "WHERE OverallRating>=" + newOverallRating + " " +
                                     "AND OverallRating<" + oldOverallRating + " " +
                                     "AND DraftRatingNumber!=" + draftRatingNumber);
                   }
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Draftrating not moved (current), reason unknown",false,context);	
                   }
                 }
               }
          }
        catch(SQLException error)
          {
	      Routines.writeToLog(servletName,"Unable to move draftratings : " + error,false,context);	
          }
        if(moveRequested)
          {
          session.setAttribute("message","Move successfull");
          }
        else
          {
          session.setAttribute("message","No players selected");
          }
        success=true;
        }
      if("Move Player Down".equals(action))
        {
        boolean moveRequested=false;
        try
          {
          int updates=0;
          Statement sql1=database.createStatement();
          Statement sql2=database.createStatement();
          Statement sql3=database.createStatement();
          ResultSet queryResult1;
          ResultSet queryResult2;
          ResultSet queryResult3;
          if(positionNumber==0)
            {
            queryResult1=sql1.executeQuery("SELECT DraftRatingNumber,PositionNumber " +
                                           "FROM draftratings,players " +
                                           "WHERE draftratings.TeamNumber=" + teamNumber + " " +
                                           "AND draftratings.PlayerNumber=players.PlayerNumber " +
                                           "ORDER BY OverallRating DESC");
            }
          else
            {
            queryResult1=sql1.executeQuery("SELECT DraftRatingNumber,PositionNumber " +
                                           "FROM draftratings,players " +
                                           "WHERE draftratings.PlayerNumber=players.PlayerNumber " +
                                           "AND players.PositionNumber=" + positionNumber + " " +
                                           "AND draftratings.TeamNumber=" + teamNumber + " " +
                                           "ORDER BY PositionRating DESC");
            }
          while(queryResult1.next())
               {
               int draftRatingNumber=queryResult1.getInt(1);
               int oldDraftRatingNumber=0;
               int currentPositionNumber=queryResult1.getInt(2);
               int currentSequence=0;
               if("true".equals(request.getParameter(String.valueOf(draftRatingNumber))))
                 {
                 if(!moveRequested)
                   {
                   moveRequested=true;
                   }
                 if(positionNumber==0)
                   {
                   queryResult2=sql2.executeQuery("SELECT OverallRating,Forname,Surname,PositionName,OverallRating " +
                                                  "FROM draftratings,positions,players " +
                                                  "WHERE players.PositionNumber=positions.PositionNumber " +
                                                  "AND draftratings.PlayerNumber=players.PlayerNumber " +
                                                  "AND DraftRatingNumber=" + draftRatingNumber);
                   }
                 else
                   {
                   queryResult2=sql2.executeQuery("SELECT PositionRating,Forname,Surname,PositionName,OverallRating " +
                                                  "FROM draftratings,positions,players " +
                                                  "WHERE players.PositionNumber=positions.PositionNumber " +
                                                  "AND draftratings.PlayerNumber=players.PlayerNumber " +
                                                  "AND DraftRatingNumber=" + draftRatingNumber);
                   }
                 queryResult2.first();
                 currentSequence=queryResult2.getInt(1);
                 int oldOverallRating=queryResult2.getInt(5);
                 int newOverallRating=0;
                 if(currentSequence==sequence)
                   {
                   session.setAttribute("message",queryResult2.getString(2) + " " + queryResult2.getString(3) + " is already at the bottom of the " + queryResult2.getString(4) + " list");
                   return false;
                   }
                 int tempPositionNumber=0;
                 if(positionNumber==0)
                   {
                   queryResult3=sql3.executeQuery("SELECT PositionNumber " +
                                                  "FROM draftratings,players " +
                                                  "WHERE draftratings.TeamNumber=" + teamNumber + " " +
                                                  "AND draftratings.PlayerNumber=players.PlayerNumber " +
                                                  "AND OverallRating=" + (currentSequence+1));
                   queryResult3.first();
                   tempPositionNumber=queryResult3.getInt(1);
                   if(currentPositionNumber==tempPositionNumber)
                     {
                     updates=sql1.executeUpdate("UPDATE draftratings " +
                                                "SET OverallRating=(OverallRating-1),PositionRating=(PositionRating-1),DateTimeStamp='" +
                                                Routines.getDateTime(false) + "' " +
                                                "WHERE TeamNumber=" + teamNumber + " " +
                                                "AND OverallRating=" + (currentSequence+1));
                     }
                   else
                     {
                     updates=sql1.executeUpdate("UPDATE draftratings " +
                                                "SET OverallRating=(OverallRating-1),DateTimeStamp='" +
                                                Routines.getDateTime(false) + "' " +
                                                "WHERE TeamNumber=" + teamNumber + " " +
                                                "AND OverallRating=" + (currentSequence+1));
                     }
                   }
                 else
                   {
                   queryResult2=sql2.executeQuery("SELECT DraftRatingNumber,OverallRating " +
                                                  "FROM draftratings,players " +
                                                  "WHERE draftratings.PlayerNumber=players.PlayerNumber " +
                                                  "AND PositionNumber=" + positionNumber + " " +
                                                  "AND draftratings.TeamNumber=" + teamNumber + " " +
                                                  "AND PositionRating=" + (currentSequence+1));
                   queryResult2.first();
                   oldDraftRatingNumber=queryResult2.getInt(1);
                   newOverallRating=queryResult2.getInt(2);
                   updates=sql1.executeUpdate("UPDATE draftratings " +
                                              "SET PositionRating=(PositionRating-1),DateTimeStamp='" +
                                              Routines.getDateTime(false) + "' " +
                                              "WHERE DraftRatingNumber=" + oldDraftRatingNumber);
                   }
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Draftrating not moved (prior), reason unknown",false,context);	
                   }
                 if(positionNumber==0)
                   {
                   updates=sql1.executeUpdate("UPDATE draftratings " +
                                              "SET OverallRating=(OverallRating+1),DateTimeStamp='" +
                                              Routines.getDateTime(false)  + "' " +
                                              "WHERE DraftRatingNumber=" + draftRatingNumber);
                   if(currentPositionNumber==tempPositionNumber)
                     {
                     updates=sql1.executeUpdate("UPDATE draftratings " +
                                                "SET PositionRating=(PositionRating+1),DateTimeStamp='" +
                                                Routines.getDateTime(false) + "' " +
                                                "WHERE DraftRatingNumber=" + draftRatingNumber);
                     }
                   }
                 else
                   {
                   updates=sql1.executeUpdate("UPDATE draftratings " +
                                              "SET PositionRating=(PositionRating+1),DateTimeStamp='" +
                                              Routines.getDateTime(false)  + "'," +
                                              "OverallRating=" + newOverallRating + " " +
                                              "WHERE DraftRatingNumber=" + draftRatingNumber);
                   sql1.executeUpdate("UPDATE draftratings " +
                                      "SET OverallRating=(OverallRating-1),DateTimeStamp='" +
                                      Routines.getDateTime(false)  + "' " +
                                      "WHERE OverallRating>" + newOverallRating + " " +
                                      "AND OverallRating<=" + oldOverallRating + " " +
                                      "AND DraftRatingNumber!=" + draftRatingNumber);
                   }
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Draftrating not moved (current), reason unknown",false,context);	
                   }
                 }
               }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to move draftratings : " + error,false,context);	
          }
        if(moveRequested)
          {
          session.setAttribute("message","Move successfull");
          }
        else
          {
          session.setAttribute("message","No players selected");
          }
        success=true;
        }
      return success;
      }
}