import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class Formations extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="Formations";

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
      int type=Routines.safeParseInt(request.getParameter("type"));
      String action=request.getParameter("action");
      String[] types={"Offense","Defense","Special Teams"};
      String disabledText="";
      if("New Formation".equals(action)||
         "Change Formation".equals(action))
         {
         disabledText=" DISABLED";
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
      if(Routines.loginCheck(true,request,response,database,context))
        {
        return;
        }
      Routines.WriteHTMLHead("Formations",//title
                             false,//showMenu
                             11,//menuHighLight
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
      int formationNumber=0;
      webPageOutput.println("<CENTER>");
      webPageOutput.println("<IMG SRC=\"../Images/EnterData.gif\"" +
                            " WIDTH='256' HEIGHT='40' ALT='Enter Data'>");
      webPageOutput.println("</CENTER>");
      if("Change Formation".equals(action))
        {
        boolean changeRequested=false;
        int changeCount=0;
        try
          {
          Statement sql=database.createStatement();
          ResultSet queryResult;
          queryResult=sql.executeQuery("SELECT FormationNumber " +
                                       "FROM formations " +
                                       "WHERE Type=" + type);
          while(queryResult.next())
               {
               formationNumber=queryResult.getInt(1);
               if("true".equals(request.getParameter(String.valueOf(formationNumber))))
                 {
                 changeCount++;
                 if(!changeRequested)
                   {
                   changeRequested=true;
                   }
                 }
               }
          }
        catch(SQLException error)
          {
          Routines.writeToLog(servletName,"Unable to find formations entries : " + error,false,context);
          }
        if(!changeRequested)
          {
          session.setAttribute("message","No formation selected");
          disabledText="";
          action="";
          }
        if(changeCount>1)
          {
          session.setAttribute("message","Please select only one formation to change");
          disabledText="";
          action="";
          }
        }
      boolean updated=true;
      if ("Store New Formation".equals(action)||
          "Store Changed Formation".equals(action)||
          "Delete Formation".equals(action)||
          "Move Formation Up".equals(action)||
          "Move Formation Down".equals(action))
          {
          updated=updateEntry(action,
                              type,
                              session,
                              request,
                              database);
          }
      webPageOutput.println("<FORM ACTION=\"http://" +
                             request.getServerName() +
                             ":" +
                             request.getServerPort() +
                             request.getContextPath() +
                             "/servlet/Formations\" METHOD=\"POST\">");
      Routines.tableStart(false,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      if(!updated)
        {
        disabledText="";
        }
      webPageOutput.println("<SELECT" + disabledText + " NAME=\"type\">");
      String selected="";
      for(int currentType=0;currentType<types.length;currentType++)
         {
         if(currentType==type)
           {
           selected=" SELECTED";
           }
         else
           {
           selected="";
           }
         webPageOutput.println(" <OPTION" + selected + " VALUE=\"" + currentType + "\">" + types[currentType]);
         }
      webPageOutput.println("</SELECT>");
      webPageOutput.println("<INPUT" + disabledText + " TYPE=\"SUBMIT\" NAME=\"action\" VALUE=\"View\">");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
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
      if("New Formation".equals(action)||
         "Change Formation".equals(action))
        {
        formScreen(action,
                   type,
                   database,
                   session,
                   request,
                   webPageOutput);
        }
      else
        {
        viewScreen(action,
                   type,
                   updated,
                   session,
                   database,
                   request,
                   response,
                   webPageOutput);
        }
      pool.returnConnection(database);
      Routines.WriteHTMLTail(request,response,webPageOutput);
      }

   private void viewScreen(String action,
                           int type,
                           boolean updated,
                           HttpSession session,
                           Connection database,
                           HttpServletRequest request,
                           HttpServletResponse response,
                           PrintWriter webPageOutput)
      {
      String currentFormation="";
      int leftEnd=0;
      int rightEnd=0;
      int formationNumber=0;
      Routines.tableStart(false,webPageOutput);
      String titleText="";
      if(type==0)
        {
        titleText="Offensive";
        }
      if(type==1)
        {
        titleText="Defensive";
        }
      if(type==2)
        {
        titleText="Special Teams";
        }
      Routines.tableHeader(titleText + " Formations",2,webPageOutput);
      boolean formationsFound=false;
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT FormationNumber,FormationName,Position2,Position8 " +
                                     "FROM formations " +
                                     "WHERE Type=" + type + " " +
                                     "ORDER BY Sequence ASC");
        formationNumber=0;
        String formationName="";
        while(queryResult.next())
             {
             if(!formationsFound)
               {
               formationsFound=true;
               }
             formationNumber=queryResult.getInt(1);
             formationName=queryResult.getString(2);
             leftEnd=queryResult.getInt(3);
             rightEnd=queryResult.getInt(4);
             String strongText="";
             if(type==0)
               {
               if(leftEnd!=0&&rightEnd==0)
                 {
                 strongText=" (Left)";
                 }
               if(leftEnd==0&&rightEnd!=0)
                 {
                 strongText=" (Right)";
                 }
               }
             Routines.tableDataStart(true,false,false,true,false,5,0,"scoresrow",webPageOutput);
             boolean selected=false;
             String param="";
             if(!updated)
               {
               param=request.getParameter(String.valueOf(formationNumber));
               if("true".equals(param))
                 {
                 selected=true;
                 }
               }
             webPageOutput.print("<INPUT TYPE=\"CHECKBOX\" NAME=\"" + formationNumber  + "\" VALUE=\"true\"");
             if(selected)
               {
               webPageOutput.print(" CHECKED");
               }
             webPageOutput.println(">");
             Routines.tableDataEnd(false,false,false,webPageOutput);
             Routines.tableDataStart(true,false,false,false,false,95,0,"scoresrow",webPageOutput);
             webPageOutput.println(formationName+strongText);
             Routines.tableDataEnd(false,false,true,webPageOutput);
             }
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Unable to retrieve formations : " + error,false,context);
        }
      if(!formationsFound)
        {
        Routines.tableDataStart(true,true,false,true,false,0,0,"scoresrow",webPageOutput);
        webPageOutput.println("No Formations found.");
        Routines.tableDataEnd(false,false,true,webPageOutput);
        }
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Actions",0,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"New Formation\" NAME=\"action\">");
      if(formationsFound)
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Change Formation\" NAME=\"action\">");
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Delete Formation\" NAME=\"action\">");
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Move Formation Up\" NAME=\"action\">");
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Move Formation Down\" NAME=\"action\">");
        }
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"jsessionid\" VALUE=\"" + session.getId() + "\">");
      webPageOutput.println("</FORM>");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      }

      private void formScreen(String action,
                              int type,
                              Connection database,
                              HttpSession session,
                              HttpServletRequest request,
                              PrintWriter webPageOutput)
      {
      int formationNumber=0;
      int subType=0;
      int endPosition[]={0,0};
      int qbPosition=0;
      int backfieldPosition=0;
      int position[]=new int[15];
      boolean positionDoubleAllowed[]={true,true,true,true,true,
                                       false,false,false,false,false,
                                       false,true,false,false,false};
      boolean positionDoubled[]=new boolean[15];
      int dePlayers[]={0,17,18,20,19,27,26,21};
      int dtPlayers[]={0,18,17,20,19,27,26,21};
      int ntPlayers[]={0,18,17,20,19};
      int obPlayers[]={0,20,19,27,26,21};
      int ibPlayers[]={0,19,20,27,26,21};
      int cbPlayers[]={0,21,26,27};
      int fsPlayers[]={0,26,27,21};
      int ssPlayers[]={0,27,26,21};
      int maxPositionNumber=27;
      String formationName="";
      Routines.tableStart(false,webPageOutput);
      String titleText="";
      String ends[]={"Left End","Right End"};
      String endPositions[]={"Tight End","Wide Receiver"};
      String qbPositions[]={"Normal","Shotgun"};
      String backfieldPositions[]={"One Back (RB)","One Back (FB)",
                                   "Pro Set (FB Strongside)","Pro Set (FB Weakside)",
                                   "Pro Set (Dual FB)","Pro Set (Dual RB)",
                                   "I (FB Leading)","I (Dual FB)",
                                   "Wishbone (Single FB)","Wishbone (Dual FB)"};
      String dePositions[]={" ","Defensive End","Defensive Tackle",
                            "Outside Linebacker","Middle Linebacker",
                            "Strong Safety","Free Safety","Cornerback"};
      String dtPositions[]={" ","Defensive Tackle","Defensive End",
                            "Outside Linebacker","Middle Linebacker",
                            "Strong Safety","Free Safety","Cornerback"};
      String ntPositions[]={" ","Defensive Tackle","Defensive End",
                            "Outside Linebacker","Middle Linebacker"};
      String obPositions[]={" ","Outside Linebacker","Middle Linebacker",
                            "Strong Safety","Free Safety","Cornerback"};
      String ibPositions[]={" ","Middle Linebacker","Outside Linebacker",
                            "Strong Safety","Free Safety","Cornerback"};
      String cbPositions[]={" ","Cornerback","Free Safety","Strong Safety"};
      String ssPositions[]={" ","Strong Safety","Free Safety","Cornerback"};
      String fsPositions[]={" ","Free Safety","Strong Safety","Cornerback"};
      String defensivePositions[]={"Left Defensive End","Left Defensive Tackle","Nose Tackle",
                                   "Right Defensive Tackle","Right Defensive End",
                                   "Outside Left Linebacker","Inside Left Linebacker",
                                   "Middle Linebacker","Inside Right Linebacker","Outside Right Linebacker",
                                   "Left Cornerback","Nickel Back","Free Safety","Strong Safety","Right Cornerback"};
      String subTypes[]={"Kick Off","Field Goal","Punt",
                         "Kick Return","Field Goal Block","Punt Return","Punt Block"};
      String selectedText="";
      if(type==0)
        {
        titleText="Offensive";
        }
      if(type==1)
        {
        titleText="Defensive";
        }
      if(type==2)
        {
        titleText="Special Teams";
        }
      if("Change Formation".equals(action))
        {
        try
          {
          Statement sql=database.createStatement();
          ResultSet queryResult;
          queryResult=sql.executeQuery("SELECT FormationNumber " +
                                       "FROM formations " +
                                       "WHERE Type=" + type + " " +
                                       "ORDER BY Sequence DESC");
          int tempFormationNumber=0;
          while(queryResult.next())
               {
               tempFormationNumber=queryResult.getInt(1);
               int currentSequence=0;
               if("true".equals(request.getParameter(String.valueOf(tempFormationNumber))))
                 {
                 queryResult=sql.executeQuery("SELECT FormationNumber,FormationName,SubType," +
                                              "Position1,Position2,Position3,Position4,Position5," +
                                              "Position6,Position7,Position8,Position9,Position10," +
                                              "Position11,Position12,Position13,Position14,Position15 " +
                                              "FROM formations " +
                                              "WHERE FormationNumber=" + tempFormationNumber);
                 if(queryResult.first())
                   {
                   formationNumber=queryResult.getInt(1);
                   formationName=queryResult.getString(2);
                   subType=queryResult.getInt(3);
                   position[0]=queryResult.getInt(4);
                   position[1]=queryResult.getInt(5);
                   position[2]=queryResult.getInt(6);
                   position[3]=queryResult.getInt(7);
                   position[4]=queryResult.getInt(8);
                   position[5]=queryResult.getInt(9);
                   position[6]=queryResult.getInt(10);
                   position[7]=queryResult.getInt(11);
                   position[8]=queryResult.getInt(12);
                   position[9]=queryResult.getInt(13);
                   position[10]=queryResult.getInt(14);
                   position[11]=queryResult.getInt(15);
                   position[12]=queryResult.getInt(16);
                   position[13]=queryResult.getInt(17);
                   position[14]=queryResult.getInt(18);
                   if(type==0)
                     {
                     if(position[0]==13)
                       {
                       endPosition[0]=1;
                       }
                     if(position[8]==13)
                       {
                       endPosition[1]=1;
                       }
                     if(position[14]==14)
                       {
                       qbPosition=1;
                       }
                     if(position[12]==15&&position[13]==0)
                       {
                       backfieldPosition=0;
                       }
                     if(position[12]==16&&position[13]==0)
                       {
                       backfieldPosition=1;
                       }
                     if(position[1]==12&&position[12]==16&&position[13]==15)
                       {
                       backfieldPosition=2;
                       }
                     if(position[7]==12&&position[12]==15&&position[13]==16)
                       {
                       backfieldPosition=2;
                       }
                     if(position[1]==12&&position[12]==15&&position[13]==16)
                       {
                       backfieldPosition=3;
                       }
                     if(position[7]==12&&position[12]==16&&position[13]==15)
                       {
                       backfieldPosition=3;
                       }
                     if(position[12]==16&&position[13]==16&&(position[14]==0||position[14]==14))
                       {
                       backfieldPosition=4;
                       }
                     if(position[12]==15&&position[13]==15&&(position[14]==0||position[14]==14))
                       {
                       backfieldPosition=5;
                       }
                     if(position[12]==0&&position[13]==16&&position[14]==15)
                       {
                       backfieldPosition=6;
                       }
                     if(position[12]==0&&position[13]==16&&position[14]==16)
                       {
                       backfieldPosition=7;
                       }
                     if(position[12]==16&&position[13]==15&&position[14]==15)
                       {
                       backfieldPosition=8;
                       }
                     if(position[12]==16&&position[13]==16&&position[14]==15)
                       {
                       backfieldPosition=9;
                       }
                     }
                   if(type==1)
                     {
                     for(int currentPosition=0;currentPosition<position.length;currentPosition++)
                        {
                        if(position[currentPosition]<0)
                          {
                          position[currentPosition]*=-1;
                          positionDoubled[currentPosition]=true;
                          }
                        }
                     }
                   }
                 else
                   {
                   Routines.writeToLog(servletName,"Unable to find formation (" + tempFormationNumber + ")",false,context);
                   }
                 }
               }
            }
       catch(SQLException error)
            {
            Routines.writeToLog(servletName,"Unable to retrieve formation: " + error,false,context);
            }
      Routines.tableHeader("Amend details of " + titleText + " formation",3,webPageOutput);
      }
      if("New Formation".equals(action))
        {
        Routines.tableHeader("Enter details of new " + titleText + " formation",3,webPageOutput);
        }
      if(type!=2)
        {
        Routines.tableDataStart(true,false,false,true,false,30,0,"scoresrow",webPageOutput);
        webPageOutput.print("Name");
        Routines.tableDataEnd(false,false,false,webPageOutput);
        Routines.tableDataStart(true,false,false,false,false,30,0,"scoresrow",webPageOutput);
        webPageOutput.print("<INPUT TYPE=\"TEXT\" NAME=\"formationName\" SIZE=\"30\" MAXLENGTH=\"30\" VALUE=\"" + formationName + "\">");
        if(type==1)
          {
          Routines.tableDataEnd(false,false,false,webPageOutput);
          Routines.tableDataStart(true,false,false,false,false,40,0,"scoresrow",webPageOutput);
          }
        Routines.tableDataEnd(false,false,true,webPageOutput);
        }
      if(type==0)
        {
        for(int currentEnd=0;currentEnd<ends.length;currentEnd++)
           {
           Routines.tableDataStart(true,false,false,true,false,10,0,"scoresrow",webPageOutput);
           webPageOutput.print(ends[currentEnd]);
           Routines.tableDataEnd(false,false,false,webPageOutput);
           Routines.tableDataStart(true,false,false,false,false,10,0,"scoresrow",webPageOutput);
           webPageOutput.println("<SELECT NAME=\"end" + currentEnd + "\">");
           for(int currentPosition=0;currentPosition<endPositions.length;currentPosition++)
              {
              if(endPosition[currentEnd]==currentPosition)
                {
                selectedText=" SELECTED";
                }
              else
                {
                selectedText="";
                }
              webPageOutput.println(" <OPTION" + selectedText + " VALUE=\"" +
                                    currentPosition + "\">" +
                                    endPositions[currentPosition]);
              }
           webPageOutput.println("</SELECT>");
           Routines.tableDataEnd(false,false,true,webPageOutput);
           }
        Routines.tableDataStart(true,false,false,true,false,10,0,"scoresrow",webPageOutput);
        webPageOutput.print("QuarterBack");
        Routines.tableDataEnd(false,false,false,webPageOutput);
        Routines.tableDataStart(true,false,false,false,false,10,0,"scoresrow",webPageOutput);
        webPageOutput.println("<SELECT NAME=\"qb\">");
        for(int currentPosition=0;currentPosition<qbPositions.length;currentPosition++)
           {
           if(qbPosition==currentPosition)
             {
             selectedText=" SELECTED";
             }
           else
             {
             selectedText="";
             }
           webPageOutput.println(" <OPTION" + selectedText + " VALUE=\"" +
                                 currentPosition + "\">" +
                                 qbPositions[currentPosition]);
           }
        webPageOutput.println("</SELECT>");
        Routines.tableDataEnd(false,false,true,webPageOutput);
        Routines.tableDataStart(true,false,false,true,false,10,0,"scoresrow",webPageOutput);
        webPageOutput.print("Backfield");
        Routines.tableDataEnd(false,false,false,webPageOutput);
        Routines.tableDataStart(true,false,false,false,false,10,0,"scoresrow",webPageOutput);
        webPageOutput.println("<SELECT NAME=\"backfield\">");
        for(int currentPosition=0;currentPosition<backfieldPositions.length;currentPosition++)
           {
           if(backfieldPosition==currentPosition)
             {
             selectedText=" SELECTED";
             }
           else
             {
             selectedText="";
             }
           webPageOutput.println(" <OPTION" + selectedText + " VALUE=\"" +
                                 currentPosition + "\">" +
                                 backfieldPositions[currentPosition]);
           }
        webPageOutput.println("</SELECT>");
        Routines.tableDataEnd(false,false,true,webPageOutput);
        }
      if(type==1)
        {
        for(int currentPosition=0;currentPosition<defensivePositions.length;currentPosition++)
           {
           Routines.tableDataStart(true,false,false,true,false,10,0,"scoresrow",webPageOutput);
           webPageOutput.print(defensivePositions[currentPosition]);
           Routines.tableDataEnd(false,false,false,webPageOutput);
           Routines.tableDataStart(true,false,false,false,false,10,0,"scoresrow",webPageOutput);
           webPageOutput.println("<SELECT NAME=\"position" + currentPosition + "\">");
           if(currentPosition==0||currentPosition==4)
             {
             for(int currentDE=0;currentDE<dePositions.length;currentDE++)
                {
                if(position[currentPosition]==dePlayers[currentDE])
                  {
                  selectedText=" SELECTED";
                  }
                else
                  {
                  selectedText="";
                  }
                webPageOutput.println(" <OPTION" + selectedText + " VALUE=\"" +
                                      dePlayers[currentDE] + "\">" +
                                      dePositions[currentDE]);
                }
             }
           if(currentPosition==1||currentPosition==3)
             {
             for(int currentDT=0;currentDT<dtPositions.length;currentDT++)
                {
                if(position[currentPosition]==dtPlayers[currentDT])
                  {
                  selectedText=" SELECTED";
                  }
                else
                  {
                  selectedText="";
                  }
                webPageOutput.println(" <OPTION" + selectedText + " VALUE=\"" +
                                      dtPlayers[currentDT] + "\">" +
                                      dtPositions[currentDT]);
                }
             }
           if(currentPosition==2)
             {
             for(int currentNT=0;currentNT<ntPositions.length;currentNT++)
                {
                if(position[currentPosition]==ntPlayers[currentNT])
                  {
                  selectedText=" SELECTED";
                  }
                else
                  {
                  selectedText="";
                  }
                webPageOutput.println(" <OPTION" + selectedText + " VALUE=\"" +
                                      ntPlayers[currentNT] + "\">" +
                                      ntPositions[currentNT]);
                }
             }
           if(currentPosition==5||currentPosition==9)
             {
             for(int currentOB=0;currentOB<obPositions.length;currentOB++)
                {
                if(position[currentPosition]==obPlayers[currentOB])
                  {
                  selectedText=" SELECTED";
                  }
                else
                  {
                  selectedText="";
                  }
                webPageOutput.println(" <OPTION" + selectedText + " VALUE=\"" +
                                      obPlayers[currentOB] + "\">" +
                                      obPositions[currentOB]);
                }
             }
           if(currentPosition==6||currentPosition==7||currentPosition==8)
             {
             for(int currentIB=0;currentIB<ibPositions.length;currentIB++)
                {
                if(position[currentPosition]==ibPlayers[currentIB])
                  {
                  selectedText=" SELECTED";
                  }
                else
                  {
                  selectedText="";
                  }
                webPageOutput.println(" <OPTION" + selectedText + " VALUE=\"" +
                                      ibPlayers[currentIB] + "\">" +
                                      ibPositions[currentIB]);
                }
             }
           if(currentPosition==10||currentPosition==11||currentPosition==14)
             {
             for(int currentCB=0;currentCB<cbPositions.length;currentCB++)
                {
                if(position[currentPosition]==cbPlayers[currentCB])
                  {
                  selectedText=" SELECTED";
                  }
                else
                  {
                  selectedText="";
                  }
                webPageOutput.println(" <OPTION" + selectedText + " VALUE=\"" +
                                      cbPlayers[currentCB] + "\">" +
                                      cbPositions[currentCB]);
                }
             }
           if(currentPosition==12)
             {
             for(int currentFS=0;currentFS<fsPositions.length;currentFS++)
                {
                if(position[currentPosition]==fsPlayers[currentFS])
                  {
                  selectedText=" SELECTED";
                  }
                else
                  {
                  selectedText="";
                  }
                webPageOutput.println(" <OPTION" + selectedText + " VALUE=\"" +
                                      fsPlayers[currentFS] + "\">" +
                                      fsPositions[currentFS]);
                }
             }
           if(currentPosition==13)
             {
             for(int currentSS=0;currentSS<ssPositions.length;currentSS++)
                {
                if(position[currentPosition]==ssPlayers[currentSS])
                  {
                  selectedText=" SELECTED";
                  }
                else
                  {
                  selectedText="";
                  }
                webPageOutput.println(" <OPTION" + selectedText + " VALUE=\"" +
                                      ssPlayers[currentSS] + "\">" +
                                      ssPositions[currentSS]);
                }
             }
           webPageOutput.println("</SELECT>");
           Routines.tableDataEnd(false,false,false,webPageOutput);
           Routines.tableDataStart(true,false,false,false,false,10,0,"scoresrow",webPageOutput);
           if(positionDoubleAllowed[currentPosition])
             {
             webPageOutput.print("Put two players into this position <INPUT TYPE=\"CHECKBOX\" NAME=\"positionDoubled" + currentPosition  + "\" VALUE=\"true\"");
             if(positionDoubled[currentPosition])
               {
               webPageOutput.print(" CHECKED");
               }
             webPageOutput.println(">");
             }
           Routines.tableDataEnd(false,false,true,webPageOutput);
           }
        }
      if(type==2)
        {
        Routines.tableDataStart(true,false,false,true,false,10,0,"scoresrow",webPageOutput);
        webPageOutput.print("Formation");
        Routines.tableDataEnd(false,false,false,webPageOutput);
        Routines.tableDataStart(true,false,false,false,false,90,0,"scoresrow",webPageOutput);
        webPageOutput.println("<SELECT NAME=\"subType\">");
        for(int currentFormation=0;currentFormation<subTypes.length;currentFormation++)
           {
           if(subType==currentFormation)
             {
             selectedText=" SELECTED";
             }
           else
             {
             selectedText="";
             }
           webPageOutput.println(" <OPTION" + selectedText + " VALUE=\"" +
                                 currentFormation + "\">" +
                                 subTypes[currentFormation]);
           }
        webPageOutput.println("</SELECT>");
        Routines.tableDataEnd(false,false,true,webPageOutput);
        }
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Actions",1,webPageOutput);
      Routines.tableDataStart(true,true,false,true,false,0,0,"scoresrow",webPageOutput);
      if("New Formation".equals(action))
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Store New Formation\" NAME=\"action\">");
        }
      else
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Store Changed Formation\" NAME=\"action\">");
        }
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Cancel\" NAME=\"action\">");
      Routines.tableDataEnd(false,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"jsessionid\" VALUE=\"" + session.getId() + "\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"formationNumber\" VALUE=\"" + formationNumber + "\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"type\" VALUE=\"" + type + "\">");
      webPageOutput.println("</FORM>");
      }

   private synchronized boolean updateEntry(String action,
                                            int type,
                                            HttpSession session,
                                            HttpServletRequest request,
                                            Connection database)
      {
      boolean success=false;
      boolean strongLeft=false;
      boolean positionDoubleAllowed[]={true,true,true,true,true,
                                       false,false,false,false,false,
                                       false,true,false,false,false};
      int dlPlayers[]={0,17,18,20,19,27,26,21};
      int ntPlayers[]={0,18,17,20,19};
      int lbPlayers[]={0,20,19,27,26,21};
      int dbPlayers[]={0,21,26,27};
      int formationNumber=Routines.safeParseInt(request.getParameter("formationNumber"));
      int subType=Routines.safeParseInt(request.getParameter("subType"));
      int sequence=0;
      String formationName=request.getParameter("formationName");
      int position[]=new int[15];
      int ends[]=new int[2];
      int qb=Routines.safeParseInt(request.getParameter("qb"));
      int backField=Routines.safeParseInt(request.getParameter("backfield"));
      int playerCount=0;
      int dlCount=0;
      for(int currentPosition=0;currentPosition<position.length;currentPosition++)
         {
         position[currentPosition]=Routines.safeParseInt(request.getParameter("position" + currentPosition));
         if(position[currentPosition]!=0)
           {
           playerCount++;
           if(currentPosition<5)
             {
             dlCount++;
             }
           }
         if("true".equals(request.getParameter("positionDoubled" + currentPosition)))
           {
           if(!positionDoubleAllowed[currentPosition])
             {
             session.setAttribute("message","Doubling up of position not allowed (" + currentPosition + ")");
             return false;
             }
           if(position[currentPosition]!=0)
             {
             playerCount++;
             if(currentPosition<5)
               {
               dlCount++;
               }
             }
           position[currentPosition]*=-1;
           }
         }
      for(int currentPosition=0;currentPosition<ends.length;currentPosition++)
         {
         ends[currentPosition]=Routines.safeParseInt(request.getParameter("end" + currentPosition));
         }
      String subTypes[]={"Kick Off","Field Goal","Punt",
                         "Kick Return","Field Goal Block","Punt Return","Punt Block"};
      int koPositions[]={35,35,35,35,35,35,35,35,35,35,1,0,0,0,0};
      int fgPositions[]={0,9,9,8,7,10,11,11,0,0,12,12,0,14,1};
      int pnPositions[]={0,12,9,8,7,10,11,12,0,0,21,21,20,0,2};
      int krPositions[]={35,35,35,35,35,35,35,0,35,35,0,4,0,4,0};
      int fgbPositions[]={-21,-17,-18,-17,-21,0,0,20,0,0,0,0,0,0,0};
      int prPositions[]={21,17,18,17,21,20,0,12,0,20,27,0,3,0,27};
      int pbPositions[]={-21,-17,18,-17,-21,0,0,20,0,0,0,0,3,0,0};

      if(qb==1&&(backField==6||backField==7))
        {
        session.setAttribute("message","Shotgun not valid from I formation");
        return false;
        }
      if(qb==1&&(backField==8||backField==9))
        {
        session.setAttribute("message","Shotgun not valid from Wishbone formation");
        return false;
        }
      if(type==2)
        {
        if(subType>subTypes.length)
          {
          session.setAttribute("message","Invalid special teams play (" + subType + ")");
          return false;
          }
        else
          {
          formationName=subTypes[subType];
          }
        }
      if(type==1&&("Store New Formation".equals(action)||"Store Changed Formation".equals(action)))
        {
        if(playerCount<11)
          {
          session.setAttribute("message","Not enough players selected (" + playerCount + ")");
          return false;
          }
        if(playerCount>11)
          {
          session.setAttribute("message","Too many players selected (" + playerCount + ")");
          return false;
          }
        if(dlCount<3)
          {
          session.setAttribute("message","Not enough linemen selected, minimum of three required (" + dlCount + ")");
          return false;
          }
        boolean validated=false;
        for(int currentPosition=0;currentPosition<position.length;currentPosition++)
           {
           validated=false;
           if(currentPosition==0||currentPosition==1||currentPosition==3||currentPosition==4)
             {
             for(int currentPlayer=0;currentPlayer<dlPlayers.length;currentPlayer++)
                {
                if(position[currentPosition]==dlPlayers[currentPlayer]||
                   position[currentPosition]*-1==dlPlayers[currentPlayer])
                  {
                  validated=true;
                  }
                }
             }
           if(currentPosition==2)
             {
             for(int currentPlayer=0;currentPlayer<ntPlayers.length;currentPlayer++)
                {
                if(position[currentPosition]==ntPlayers[currentPlayer]||
                   position[currentPosition]*-1==ntPlayers[currentPlayer])
                  {
                  validated=true;
                  }
                }
             }
           if(currentPosition>4&&currentPosition<10)
             {
             for(int currentPlayer=0;currentPlayer<lbPlayers.length;currentPlayer++)
                {
                if(position[currentPosition]==lbPlayers[currentPlayer])
                  {
                  validated=true;
                  }
                }
             }
           if(currentPosition>9)
             {
             for(int currentPlayer=0;currentPlayer<dbPlayers.length;currentPlayer++)
                {
                if(position[currentPosition]==dbPlayers[currentPlayer]||
                   position[currentPosition]*-1==dbPlayers[currentPlayer])
                  {
                  validated=true;
                  }
                }
             }
           if(!validated)
             {
             session.setAttribute("message","Invalid player/position combination (" + currentPosition + ")");
             return false;
             }
           }
        if(position[11]!=0&&(position[10]==0||position[12]==0||position[13]==0||position[14]==0))
          {
          session.setAttribute("message","Nickel Back can only be used if all other DB positions are occupied");
          return false;
          }
        int dbPlayingDL=0;
        for(int currentPosition=0;currentPosition<5;currentPosition++)
           {
           if(position[currentPosition]==21||position[currentPosition]==26||position[currentPosition]==27)
             {
             dbPlayingDL++;
             }
           if(position[currentPosition]==-21||position[currentPosition]==-26||position[currentPosition]==-27)
             {
             dbPlayingDL+=2;
             }
           }
        if(dbPlayingDL>1)
          {
          session.setAttribute("message","Max of one DB can be lined up at DL (" + dbPlayingDL + ")");
          return false;
          }
        int dbPlayingLB=0;
        for(int currentPosition=5;currentPosition<10;currentPosition++)
           {
           if(position[currentPosition]==21||position[currentPosition]==26||position[currentPosition]==27)
             {
             dbPlayingLB++;
             }
           }
        if(dbPlayingLB>2)
          {
          session.setAttribute("message","Max of two DB can be lined up at LB (" + dbPlayingLB + ")");
          return false;
          }
        }
      if(type==0)
        {
        position[2]=9;
        position[3]=8;
        position[4]=7;
        position[5]=10;
        position[6]=11;
        if(ends[0]==0)
          {
          position[0]=0;
          position[1]=12;
          strongLeft=true;
          }
        else
          {
          position[0]=13;
          position[1]=0;
          }
        if(ends[1]==0)
          {
          position[7]=12;
          position[8]=0;
          }
        else
          {
          position[7]=0;
          position[8]=13;
          }
        if(qb==0)
          {
          position[9]=14;
          }
        else
          {
          position[14]=14;
          }
        //One Back(RB)
        if(backField==0)
          {
          position[10]=13;
          position[11]=13;
          position[12]=15;
          }
        //One Back(FB)
        if(backField==1)
          {
          position[10]=13;
          position[11]=13;
          position[12]=16;
          }
        //Pro Set (FB Strongside)
        if(backField==2)
          {
          if(strongLeft)
            {
            position[10]=13;
            position[12]=16;
            position[13]=15;
            }
          else
            {
            position[11]=13;
            position[12]=15;
            position[13]=16;
            }
          }
        //Pro Set (FB Weakside)
        if(backField==3)
          {
          if(strongLeft)
            {
            position[10]=13;
            position[12]=15;
            position[13]=16;
            }
          else
            {
            position[11]=13;
            position[12]=16;
            position[13]=15;
            }
          }
        //Pro Set (Dual FB)
        if(backField==4)
          {
          if(strongLeft)
            {
            position[10]=13;
            }
          else
            {
            position[11]=13;
            }
          position[12]=16;
          position[13]=16;
          }
        //Pro Set (Dual RB)
        if(backField==5)
          {
          if(strongLeft)
            {
            position[10]=13;
            }
          else
            {
            position[11]=13;
            }
          position[12]=15;
          position[13]=15;
          }
        //I (FB Leading)
        if(backField==6)
          {
          if(strongLeft)
            {
            position[10]=13;
            }
          else
            {
            position[11]=13;
            }
          position[13]=16;
          position[14]=15;
          }
        //I (Dual FB)
        if(backField==7)
          {
          if(strongLeft)
            {
            position[10]=13;
            }
          else
            {
            position[11]=13;
            }
          position[13]=16;
          position[14]=16;
          }
        //Wishbone(Single FB)
        if(backField==8)
          {
          position[12]=16;
          position[13]=15;
          position[14]=15;
          }
        //Wishbone(Dual FB)
        if(backField==9)
          {
          position[12]=16;
          position[13]=16;
          position[14]=15;
          }
        }
      if(type==2)
        {
        if(subType==0)
          {
          position=koPositions;
          }
        if(subType==1)
          {
          position=fgPositions;
          }
        if(subType==2)
          {
          position=pnPositions;
          }
        if(subType==3)
          {
          position=krPositions;
          }
        if(subType==4)
          {
          position=fgbPositions;
          }
        if(subType==5)
          {
          position=prPositions;
          }
        if(subType==6)
          {
          position=pbPositions;
          }
        }
      try
        {
        // Get Latest SequenceNumber.
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT Sequence " +
                                     "FROM formations " +
                                     "WHERE Type=" + type + " " +
                                     "ORDER BY Sequence DESC");
        if(queryResult.first())
          {
          sequence=queryResult.getInt(1);
          }
        if(formationNumber==0)
          {
          //Get latest FormationNumber.
          formationNumber=1;
          queryResult=sql.executeQuery("SELECT FormationNumber " +
                                       "FROM formations " +
                                       "ORDER BY FormationNumber DESC");
          if(queryResult.first())
            {
            formationNumber=queryResult.getInt(1) + 1;
            }
          }
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Unable to retrieve formations : " + error,false,context);
        }
      if("Move Formation Up".equals(action))
        {
        boolean moveRequested=false;
        try
          {
          int updates=0;
          Statement sql1=database.createStatement();
          Statement sql2=database.createStatement();
          ResultSet queryResult1;
          ResultSet queryResult2;
          queryResult1=sql1.executeQuery("SELECT FormationNumber " +
                                         "FROM formations " +
                                         "WHERE Type=" + type + " " +
                                         "ORDER BY Sequence ASC");
          while(queryResult1.next())
               {
               formationNumber=queryResult1.getInt(1);
               int currentSequence=0;
               if("true".equals(request.getParameter(String.valueOf(formationNumber))))
                 {
                 if(!moveRequested)
                   {
                   moveRequested=true;
                   }
                 queryResult2=sql2.executeQuery("SELECT Sequence,FormationName FROM formations " +
                                                "WHERE FormationNumber=" + formationNumber);
                 queryResult2.first();
                 currentSequence=queryResult2.getInt(1);
                 if(currentSequence==1)
                   {
                   session.setAttribute("message",queryResult2.getString(2) + " is already at the top of the formation list");
                   return false;
                   }
                 updates=sql1.executeUpdate("UPDATE formations " +
                                            "SET Sequence=(Sequence+1),DateTimeStamp='" +
                                            Routines.getDateTime(false) + "' " +
                                            "WHERE Type=" + type + " " +
                                            "AND Sequence=" + (currentSequence-1));
                 if(updates!=1)
                   {
                   Routines.writeToLog(servletName,"Formation not moved (prior), reason unknown",false,context);
                   }
                 updates=sql1.executeUpdate("UPDATE formations " +
                                            "SET Sequence=(Sequence-1),DateTimeStamp='" +
                                            Routines.getDateTime(false)  + "' " +
                                            "WHERE FormationNumber=" + formationNumber);
                 if(updates!=1)
                   {
                   Routines.writeToLog(servletName,"Formation not moved (current), reason unknown",false,context);
                   }
                 }
               }
          }
        catch(SQLException error)
          {
          Routines.writeToLog(servletName,"Unable to move formations : " + error,false,context);
          }
        if(moveRequested)
          {
          session.setAttribute("message","Move successfull");
          }
        else
          {
          session.setAttribute("message","No formations selected");
          }
        success=true;
        }
      if("Move Formation Down".equals(action))
        {
        boolean moveRequested=false;
        try
          {
          int updates=0;
          Statement sql1=database.createStatement();
          Statement sql2=database.createStatement();
          ResultSet queryResult1;
          ResultSet queryResult2;
          queryResult1=sql1.executeQuery("SELECT FormationNumber " +
                                         "FROM formations " +
                                         "WHERE Type=" + type + " " +
                                         "ORDER BY Sequence DESC");
          while(queryResult1.next())
               {
               formationNumber=queryResult1.getInt(1);
               int currentSequence=0;
               if("true".equals(request.getParameter(String.valueOf(formationNumber))))
                 {
                 if(!moveRequested)
                   {
                   moveRequested=true;
                   }
                 queryResult2=sql2.executeQuery("SELECT Sequence,FormationName FROM formations " +
                                                "WHERE FormationNumber=" + formationNumber);
                 queryResult2.first();
                 currentSequence=queryResult2.getInt(1);
                 if(currentSequence==sequence)
                   {
                   session.setAttribute("message",queryResult2.getString(2) + " is already at the bottom of the formation list");
                   return false;
                   }
                 updates=sql1.executeUpdate("UPDATE formations " +
                                            "SET Sequence=(Sequence-1),DateTimeStamp='" +
                                            Routines.getDateTime(false) + "' " +
                                            "WHERE Type=" + type + " " +
                                            "AND Sequence=" + (currentSequence+1));
                 if(updates!=1)
                   {
                   Routines.writeToLog(servletName,"Formation not moved (prior), reason unknown",false,context);
                   }
                 updates=sql1.executeUpdate("UPDATE formations " +
                                            "SET Sequence=(Sequence+1),DateTimeStamp='" +
                                            Routines.getDateTime(false) + "' " +
                                            "WHERE FormationNumber=" + formationNumber);
                 if(updates!=1)
                   {
                   Routines.writeToLog(servletName,"Formation not moved (current), reason unknown",false,context);
                   }
                 }
               }
          }
        catch(SQLException error)
          {
          Routines.writeToLog(servletName,"Unable to move formations : " + error,false,context);
          }
        if(moveRequested)
          {
          session.setAttribute("message","Move successfull");
          }
        else
          {
          session.setAttribute("message","No formations selected");
          }
        success=true;
        }
      if("Store New Formation".equals(action))
        {
        try
          {
          int updates=0;
          Statement sql=database.createStatement();
          ResultSet queryResult;
          updates=sql.executeUpdate("INSERT INTO formations (" +
                                    "FormationNumber,Type," +
                                    "Sequence,FormationName,SubType,DateTimeStamp," +
                                    "Position1,Position2,Position3,Position4,Position5," +
                                    "Position6,Position7,Position8,Position9,Position10," +
                                    "Position11,Position12,Position13,Position14,Position15) " +
                                    "VALUES (" +
                                    formationNumber + "," +
                                    type + "," +
                                    (sequence+1) + ",\"" +
                                    formationName + "\"," +
                                    subType + ",'" +
                                    Routines.getDateTime(false) + "'," +
                                    position[0] + "," +
                                    position[1] + "," +
                                    position[2] + "," +
                                    position[3] + "," +
                                    position[4] + "," +
                                    position[5] + "," +
                                    position[6] + "," +
                                    position[7] + "," +
                                    position[8] + "," +
                                    position[9] + "," +
                                    position[10] + "," +
                                    position[11] + "," +
                                    position[12] + "," +
                                    position[13] + "," +
                                    position[14] + ")");
          if(updates!=1)
            {
            Routines.writeToLog(servletName,"New formation not created, reason unknown",false,context);
            }
          }
        catch(SQLException error)
          {
          Routines.writeToLog(servletName,"Unable to create formations : " + error,false,context);
          }
        session.setAttribute("message",formationName + " formation stored successfully");
        success=true;
        }
      if("Store Changed Formation".equals(action))
        {
        try
          {
          int updates=0;
          Statement sql=database.createStatement();
          ResultSet queryResult;
          updates=sql.executeUpdate("UPDATE formations " +
                                    "SET FormationName='" + formationName + "'," +
                                    "SubType=" + subType + ",DateTimeStamp='" +
                                    Routines.getDateTime(false) + "',Position1=" + position[0] +
                                    ",Position2=" + position[1] +
                                    ",Position3=" + position[2] +
                                    ",Position4=" + position[3] +
                                    ",Position5=" + position[4] +
                                    ",Position6=" + position[5] +
                                    ",Position7=" + position[6] +
                                    ",Position8=" + position[7] +
                                    ",Position9=" + position[8] +
                                    ",Position10=" + position[9] +
                                    ",Position11=" + position[10] +
                                    ",Position12=" + position[11] +
                                    ",Position13=" + position[12] +
                                    ",Position14=" + position[13] +
                                    ",Position15=" + position[14] + " " +
                                    "WHERE FormationNumber=" + formationNumber);
          if(updates!=1)
            {
            Routines.writeToLog(servletName,"Formation not updated, reason unknown",false,context);
            }
          }
        catch(SQLException error)
          {
          Routines.writeToLog(servletName,"Unable to update formations : " + error,false,context);
          }
        session.setAttribute("message",formationName + " formations changed successfully");
        success=true;
        }
      if("Delete Formation".equals(action))
        {
        boolean deleteRequested=false;
        try
          {
          int updates=0;
          Statement sql1=database.createStatement();
          Statement sql2=database.createStatement();
          ResultSet queryResult1;
          ResultSet queryResult2;
          queryResult1=sql1.executeQuery("SELECT FormationNumber " +
                                         "FROM formations " +
                                         "WHERE Type=" + type);
          while(queryResult1.next())
               {
               formationNumber=queryResult1.getInt(1);
               if("true".equals(request.getParameter(String.valueOf(formationNumber))))
                 {
                 if(!deleteRequested)
                   {
                   deleteRequested=true;
                   }
                 queryResult2=sql2.executeQuery("SELECT PlayBookNumber " +
                                                "FROM defaultplaybook " +
                                                "WHERE FormationNumber=" + formationNumber);
                 if(queryResult2.first())
                   {
                   session.setAttribute("message","Formation currently in use by defaultplaybook entries");
                   return false;
                   }
                 else
                   {
                   queryResult2=sql2.executeQuery("SELECT PlayBookNumber " +
                                                  "FROM playbook " +
                                                  "WHERE FormationNumber=" + formationNumber);
                   if(queryResult2.first())
                     {
                     session.setAttribute("message","Formation currently in use by playbook entries");
                     return false;
                     }
                   else
                     {
                     queryResult2=sql2.executeQuery("SELECT PlayByPlayNumber " +
                                                    "FROM playbyplay " +
                                                    "WHERE OffensiveFormationNumber=" + formationNumber + " " +
                                                    "OR DefensiveFormationNumber=" + formationNumber);
                     if(queryResult2.first())
                       {
                       session.setAttribute("message","Formation currently in use by playbyplay entries");
                       return false;
                       }
                     else
                       {
                       updates=sql2.executeUpdate("DELETE FROM formations " +
                                                  "WHERE FormationNumber=" + formationNumber);
                       if(updates!=1)
                         {
                         Routines.writeToLog(servletName,"Formation not deleted (" + formationNumber + ")",false,context);
                         }
                       }
                     }
                   }
                 }
               }
          queryResult1=sql1.executeQuery("SELECT FormationNumber " +
                                         "FROM formations " +
                                         "WHERE Type=" + type + " " +
                                         "ORDER BY Sequence ASC");
          int newSequence=0;
          formationNumber=0;
          while(queryResult1.next())
                {
                newSequence++;
                formationNumber=queryResult1.getInt(1);
                updates=sql2.executeUpdate("UPDATE formations " +
                                           "SET Sequence=" + newSequence + ",DateTimeStamp='" +
                                           Routines.getDateTime(false) + "' " +
                                           "WHERE FormationNumber=" + formationNumber);
                 if(updates!=1)
                   {
                   Routines.writeToLog(servletName,"Formation entry not reset (" + formationNumber + ")",false,context);
                   }
                }
          }
        catch(SQLException error)
          {
          Routines.writeToLog(servletName,"Unable to delete formations : " + error,false,context);
          }
        if(deleteRequested)
          {
          session.setAttribute("message","Delete successfull");
          }
        else
          {
          session.setAttribute("message","No formations selected");
          }
        success=true;
        }
      return success;
      }
}