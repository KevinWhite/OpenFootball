import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class DefaultSituations extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="DefaultSituations";

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
      int maxCallNumber=0;
      int callNumbers[]=new int[20];
      String callNames[]=new String[20];
      String situationName="";
      int down1=0;
      int down2=0;
      int down3=0;
      int down4=0;
      int timeFrom=0;
      int timeTo=0;
      int ydsDownFrom=0;
      int ydsDownTo=0;
      int ydsScoreFrom=0;
      int ydsScoreTo=0;
      int pointsFrom=0;
      int pointsTo=0;
      int callNumber[]=new int[20];
      int playOrderNumber=0;
      int storeSituationNumber=0;
      String storeSituationName="";
      int storeDown1=0;
      int storeDown2=0;
      int storeDown3=0;
      int storeDown4=0;
      int storeTimeFrom=0;
      int storeTimeTo=0;
      int storeYdsDownFrom=0;
      int storeYdsDownTo=0;
      int storePointsFrom=0;
      int storePointsTo=0;
      int storeYdsScoreFrom=0;
      int storeYdsScoreTo=0;
      int storePlayOrderNumber=0;
      int storeCallNumber[]=new int[20];
      if("New Situation".equals(action)||
         "Change Situation".equals(action))
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
      Routines.WriteHTMLHead("Default Situations",//title
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
      int situationNumber=0;
      webPageOutput.println("<CENTER>");
      webPageOutput.println("<IMG SRC=\"../Images/EnterData.gif\"" +
                            " WIDTH='256' HEIGHT='40' ALT='Enter Data'>");
      webPageOutput.println("</CENTER>");
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT SituationCallNumber,SituationCallName " +
                                     "FROM situationcalls " +
                                     "WHERE Type=" + type + " " +
                                     "ORDER BY Sequence ASC");
        while(queryResult.next())
             {
             callNumbers[maxCallNumber]=queryResult.getInt(1);
             callNames[maxCallNumber]=queryResult.getString(2);
             maxCallNumber++;
             }
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Unable to find situationcall entries : " + error,false,context);
        }
      if("Change Situation".equals(action))
        {
        boolean changeRequested=false;
        int changeCount=0;
        try
          {
          Statement sql=database.createStatement();
          ResultSet queryResult;
          queryResult=sql.executeQuery("SELECT SituationNumber " +
                                       "FROM defaultsituations " +
                                       "WHERE Type=" + type);
          while(queryResult.next())
               {
               situationNumber=queryResult.getInt(1);
               if("true".equals(request.getParameter(String.valueOf(situationNumber))))
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
          Routines.writeToLog(servletName,"Unable to find defaultsituation entries : " + error,false,context);
          }
        if(!changeRequested)
          {
          session.setAttribute("message","No situation selected");
          disabledText="";
          action="";
          }
        if(changeCount>1)
          {
          session.setAttribute("message","Please select only one situation to change");
          disabledText="";
          action="";
          }
        }
      boolean updated=true;
      situationNumber=0;
      if ("Store New Situation".equals(action)||
          "Store Changed Situation".equals(action)||
          "Delete Situation".equals(action)||
          "Move Situation Up".equals(action)||
          "Move Situation Down".equals(action))
          {
          updated=updateEntry(action,
                              type,
                              callNumbers,
                              session,
                              request,
                              database);
          }
      if(!updated)
        {
        disabledText="";
        }
      webPageOutput.println("<FORM ACTION=\"http://" +
                             request.getServerName() +
                             ":" +
                             request.getServerPort() +
                             request.getContextPath() +
                             "/servlet/DefaultSituations\" METHOD=\"POST\">");
      Routines.tableStart(false,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      webPageOutput.println("<SELECT" + disabledText + " NAME=\"type\">");
      String selectedType="";
      for(int currentType=0;currentType<types.length;currentType++)
         {
         if(currentType==type)
           {
           selectedType=" SELECTED";
           }
         else
           {
           selectedType="";
           }
         webPageOutput.println(" <OPTION" + selectedType + " VALUE=\"" + currentType + "\">" + types[currentType]);
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
        Routines.tableHeader(titleText + " Situations",19,webPageOutput);
      boolean situationsFound=false;
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT SituationNumber,SituationName," +
                                     "Down1,Down2,Down3,Down4,TimeFrom,TimeTo," +
                                     "YdsDownFrom,YdsDownTo,YdsScoreFrom,YdsScoreTo," +
                                     "PointsFrom,PointsTo," +
                                     "SituationCallNumber1,SituationCallNumber2," +
                                     "SituationCallNumber3,SituationCallNumber4," +
                                     "SituationCallNumber5,SituationCallNumber6," +
                                     "SituationCallNumber7,SituationCallNumber8," +
                                     "SituationCallNumber9,SituationCallNumber10," +
                                     "SituationCallNumber11,SituationCallNumber12," +
                                     "SituationCallNumber13,SituationCallNumber14," +
                                     "SituationCallNumber15,SituationCallNumber16," +
                                     "SituationCallNumber17,SituationCallNumber18," +
                                     "SituationCallNumber19,SituationCallNumber20," +
                                     "PlayOrderName,defaultsituations.PlayOrderNumber " +
                                     "FROM defaultsituations,playorders " +
                                     "WHERE Type=" + type + " " +
                                     "AND defaultsituations.PlayOrderNumber=playorders.PlayOrderNumber " +
                                     "ORDER BY defaultsituations.Sequence ASC");
        situationNumber=0;
        situationName="";
        down1=0;
        down2=0;
        down3=0;
        down4=0;
        timeFrom=0;
        timeTo=0;
        ydsDownFrom=0;
        ydsDownTo=0;
        ydsScoreFrom=0;
        ydsScoreTo=0;
        pointsFrom=0;
        pointsTo=0;
        callNumber[0]=0;
        callNumber[1]=0;
        callNumber[2]=0;
        callNumber[3]=0;
        callNumber[4]=0;
        callNumber[5]=0;
        callNumber[6]=0;
        callNumber[7]=0;
        callNumber[8]=0;
        callNumber[9]=0;
        callNumber[10]=0;
        callNumber[11]=0;
        callNumber[12]=0;
        callNumber[13]=0;
        callNumber[14]=0;
        callNumber[15]=0;
        callNumber[16]=0;
        callNumber[17]=0;
        callNumber[18]=0;
        callNumber[19]=0;
        String playOrderName="";
        playOrderNumber=0;
        while(queryResult.next())
             {
             if(!situationsFound)
               {
               situationsFound=true;
               tableHeaders(type,webPageOutput);
               }
             situationNumber=queryResult.getInt(1);
             situationName=queryResult.getString(2);
             down1=queryResult.getInt(3);
             down2=queryResult.getInt(4);
             down3=queryResult.getInt(5);
             down4=queryResult.getInt(6);
             timeFrom=queryResult.getInt(7);
             timeTo=queryResult.getInt(8);
             ydsDownFrom=queryResult.getInt(9);
             ydsDownTo=queryResult.getInt(10);
             ydsScoreFrom=queryResult.getInt(11);
             ydsScoreTo=queryResult.getInt(12);
             pointsFrom=queryResult.getInt(13);
             pointsTo=queryResult.getInt(14);
             for(int currentCall=0;currentCall<20;currentCall++)
                {
                callNumber[currentCall]=queryResult.getInt(15+currentCall);
                }
             playOrderName=queryResult.getString(35);
             playOrderNumber=queryResult.getInt(36);
             boolean bold=false;
             if("Change Situation".equals(action)&&
               "true".equals(request.getParameter(String.valueOf(situationNumber))))
               {
               bold=true;
               storeSituationNumber=situationNumber;
               storeSituationName=queryResult.getString(2);
               storeDown1=queryResult.getInt(3);
               storeDown2=queryResult.getInt(4);
               storeDown3=queryResult.getInt(5);
               storeDown4=queryResult.getInt(6);
               storeTimeFrom=queryResult.getInt(7);
               storeTimeTo=queryResult.getInt(8);
               storeYdsDownFrom=queryResult.getInt(9);
               storeYdsDownTo=queryResult.getInt(10);
               storeYdsScoreFrom=queryResult.getInt(11);
               storeYdsScoreTo=queryResult.getInt(12);
               storePointsFrom=queryResult.getInt(13);
               storePointsTo=queryResult.getInt(14);
               for(int currentCall=0;currentCall<20;currentCall++)
                  {
                  storeCallNumber[currentCall]=queryResult.getInt(15+currentCall);
                  }
               storePlayOrderNumber=queryResult.getInt(36);
               }
             Routines.tableDataStart(true,false,bold,true,false,3,0,"scoresrow",webPageOutput);
             boolean selected=false;
             String param="";
             if(!updated||"Change Situation".equals(action))
               {
               param=request.getParameter(String.valueOf(situationNumber));
               if("true".equals(param))
                 {
                 selected=true;
                 }
               }
             if("Change Situation".equals(action)||
                "New Situation".equals(action))
                {
                disabledText=" DISABLED";
                }
             else
                {
                disabledText="";
                }
             webPageOutput.print("<INPUT TYPE=\"CHECKBOX\"" + disabledText + " NAME=\"" + situationNumber  + "\" VALUE=\"true\"");
             if(selected)
               {
               webPageOutput.print(" CHECKED");
               }
             webPageOutput.println(">");
             Routines.tableDataEnd(bold,false,false,webPageOutput);
             Routines.tableDataStart(true,false,bold,false,false,18,0,"scoresrow",webPageOutput);
             webPageOutput.println(situationName);
             Routines.tableDataEnd(bold,false,false,webPageOutput);
             Routines.tableDataStart(true,true,bold,false,false,3,0,"scoresrow",webPageOutput);
             webPageOutput.print("<INPUT DISABLED TYPE=\"CHECKBOX\" NAME=\"down1\" VALUE=\"true\"");
             if(down1==1)
               {
               webPageOutput.print(" CHECKED");
               }
             webPageOutput.println(">");
             Routines.tableDataEnd(bold,false,false,webPageOutput);
             Routines.tableDataStart(true,true,bold,false,false,3,0,"scoresrow",webPageOutput);
             webPageOutput.print("<INPUT DISABLED TYPE=\"CHECKBOX\" NAME=\"down2\" VALUE=\"true\"");
             if(down2==1)
               {
               webPageOutput.print(" CHECKED");
               }
             webPageOutput.println(">");
             Routines.tableDataEnd(bold,false,false,webPageOutput);
             Routines.tableDataStart(true,true,bold,false,false,3,0,"scoresrow",webPageOutput);
             webPageOutput.print("<INPUT DISABLED TYPE=\"CHECKBOX\" NAME=\"down3\" VALUE=\"true\"");
             if(down3==1)
               {
               webPageOutput.print(" CHECKED");
               }
             webPageOutput.println(">");
             Routines.tableDataEnd(bold,false,false,webPageOutput);
             Routines.tableDataStart(true,true,bold,false,false,3,0,"scoresrow",webPageOutput);
             webPageOutput.print("<INPUT DISABLED TYPE=\"CHECKBOX\" NAME=\"down4\" VALUE=\"true\"");
             if(down4==1)
               {
               webPageOutput.print(" CHECKED");
               }
             webPageOutput.println(">");
             Routines.tableDataEnd(bold,false,false,webPageOutput);
             Routines.tableDataStart(true,false,bold,false,false,5,0,"scoresrow",webPageOutput);
             switch(timeFrom)
                {
                case 0:
                webPageOutput.println("Start");
                break;
                case 15:
                webPageOutput.println("2nd");
                break;
                case 30:
                webPageOutput.println("Half");
                break;
                case 45:
                webPageOutput.println("4th");
                break;
                case 75:
                webPageOutput.println("End");
                break;
                default:
                webPageOutput.println(timeFrom+"min");
                }
             Routines.tableDataEnd(bold,false,false,webPageOutput);
             Routines.tableDataStart(true,false,bold,false,false,7,0,"scoresrow",webPageOutput);
             switch(timeTo)
                {
                case 0:
                webPageOutput.println("Start");
                break;
                case 15:
                webPageOutput.println("2nd");
                break;
                case 30:
                webPageOutput.println("Half");
                break;
                case 45:
                webPageOutput.println("4th");
                break;
                case 75:
                webPageOutput.println("End");
                break;
                default:
                webPageOutput.println(timeTo+"min");
                }
             Routines.tableDataEnd(bold,false,false,webPageOutput);
             Routines.tableDataStart(true,false,bold,false,false,3,0,"scoresrow",webPageOutput);
             Routines.tableDataEnd(bold,false,false,webPageOutput);
             Routines.tableDataStart(true,false,bold,false,false,5,0,"scoresrow",webPageOutput);
             webPageOutput.println(ydsDownFrom+"yd");
             Routines.tableDataEnd(bold,false,false,webPageOutput);
             Routines.tableDataStart(true,false,bold,false,false,7,0,"scoresrow",webPageOutput);
             switch(ydsDownTo)
                {
                case 99:
                webPageOutput.println("Any");
                break;
                default:
                webPageOutput.println(ydsDownTo+"yd");
                }
             Routines.tableDataEnd(bold,false,false,webPageOutput);
             Routines.tableDataStart(true,false,bold,false,false,3,0,"scoresrow",webPageOutput);
             Routines.tableDataEnd(bold,false,false,webPageOutput);
             Routines.tableDataStart(true,false,bold,false,false,5,0,"scoresrow",webPageOutput);
             webPageOutput.println(ydsScoreFrom+"yd");
             Routines.tableDataEnd(bold,false,false,webPageOutput);
             Routines.tableDataStart(true,false,bold,false,false,7,0,"scoresrow",webPageOutput);
             switch(ydsScoreTo)
                {
                case 99:
                webPageOutput.println("Any");
                break;
                default:
                webPageOutput.println(ydsScoreTo+"yd");
                }
             Routines.tableDataEnd(bold,false,false,webPageOutput);
             Routines.tableDataStart(true,false,bold,false,false,3,0,"scoresrow",webPageOutput);
             Routines.tableDataEnd(bold,false,false,webPageOutput);
             Routines.tableDataStart(true,false,bold,false,false,5,0,"scoresrow",webPageOutput);
             if(pointsFrom==-22)
                {
                webPageOutput.println("Dn");
                }
             else
                {
                if(pointsFrom==0)
                  {
                  webPageOutput.println("Tied");
                  }
                else
                  {
                  if(pointsFrom<0)
                    {
                    webPageOutput.println("Dn" + (pointsFrom*-1));
                    }
                  else
                    {
                    webPageOutput.println("Up" + pointsFrom);
                    }
                  }
                }
             Routines.tableDataEnd(bold,false,false,webPageOutput);
             Routines.tableDataStart(true,false,bold,false,false,7,0,"scoresrow",webPageOutput);
             if(pointsTo==0)
               {
               webPageOutput.println("Tied");
               }
             else
               {
               if(pointsTo==+22)
                 {
                 webPageOutput.println("Up");
                 }
               else
                 {
                 if(pointsTo<0)
                   {
                   webPageOutput.println("Dn" + (pointsTo*-1));
                   }
                 else
                   {
                   webPageOutput.println("Up" + pointsTo);
                   }
                 }
               }
             Routines.tableDataEnd(bold,false,false,webPageOutput);
             Routines.tableDataStart(true,false,bold,false,false,5,0,"scoresrow",webPageOutput);
             webPageOutput.println(playOrderName);
             Routines.tableDataEnd(bold,false,false,webPageOutput);
             Routines.tableDataStart(true,false,bold,false,false,5,0,"scoresrow",webPageOutput);
             if(type==2)
               {
               for(int currentCall=0;currentCall<callNames.length;currentCall++)
                  {
                  if(callNumber[0]!=0&&callNumber[0]==callNumbers[currentCall])
                    {
                    webPageOutput.println(callNames[currentCall]);
                    }
                  }
               }
             else
               {
               webPageOutput.println("<SELECT" + disabledText + " NAME=\"dummy\">");
               for(int currentCall=0;currentCall<20;currentCall++)
                  {
                  if(callNumber[currentCall]!=0)
                    {
                    for(int currentOption=0;currentOption<maxCallNumber;currentOption++)
                       {
                       if(callNumber[currentCall]==callNumbers[currentOption])
                         {
                         webPageOutput.println(" <OPTION VALUE=\"dummy\">" + callNames[currentOption]);
                         }
                       }
                    }
                  }
                }
             webPageOutput.println("</SELECT>");
             Routines.tableDataEnd(bold,false,true,webPageOutput);
             }
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Unable to retrieve defaultsituations : " + error,false,context);
        }
      if(!situationsFound)
        {
        Routines.tableDataStart(true,true,false,true,false,0,0,"scoresrow",webPageOutput);
        webPageOutput.println("No Situations found.");
        Routines.tableDataEnd(false,false,true,webPageOutput);
        }
      Routines.tableEnd(webPageOutput);
      if("New Situation".equals(action))
        {
        situationNumber=0;
        situationName="";
        down1=1;
        down2=1;
        down3=1;
        down4=1;
        timeFrom=0;
        timeTo=75;
        ydsDownFrom=1;
        ydsDownTo=99;
        ydsScoreFrom=1;
        ydsScoreTo=99;
        pointsFrom=-22;
        pointsTo=22;
        playOrderNumber=1;
        callNumber=null;
        }
      if("Change Situation".equals(action))
        {
        situationNumber=storeSituationNumber;
        situationName=storeSituationName;
        down1=storeDown1;
        down2=storeDown2;
        down3=storeDown3;
        down4=storeDown4;
        timeFrom=storeTimeFrom;
        timeTo=storeTimeTo;
        ydsDownFrom=storeYdsDownFrom;
        ydsDownTo=storeYdsDownTo;
        ydsScoreFrom=storeYdsScoreFrom;
        ydsScoreTo=storeYdsScoreTo;
        pointsFrom=storePointsFrom;
        pointsTo=storePointsTo;
        playOrderNumber=storePlayOrderNumber;
        callNumber=storeCallNumber;
        }
      if("New Situation".equals(action)||
         "Change Situation".equals(action))
        {
        formLine(action,
                 titleText,
                 situationName,
                 type,
                 down1,
                 down2,
                 down3,
                 down4,
                 timeFrom,
                 timeTo,
                 ydsDownFrom,
                 ydsDownTo,
                 ydsScoreFrom,
                 ydsScoreTo,
                 pointsFrom,
                 pointsTo,
                 playOrderNumber,
                 callNumber,
                 callNumbers,
                 callNames,
                 webPageOutput,
                 database);
        }
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Actions",0,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      if("New Situation".equals(action))
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Store New Situation\" NAME=\"action\">");
        }
      if("Change Situation".equals(action))
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Store Changed Situation\" NAME=\"action\">");
        }
      if("New Situation".equals(action)||
         "Change Situation".equals(action))
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Cancel\" NAME=\"action\">");
        webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"situationNumber\" VALUE=\"" + situationNumber + "\">");
        webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"type\" VALUE=\"" + type + "\">");
        }
      else
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"New Situation\" NAME=\"action\">");
        if(situationsFound)
          {
          webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Change Situation\" NAME=\"action\">");
          webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Delete Situation\" NAME=\"action\">");
          webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Move Situation Up\" NAME=\"action\">");
          webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Move Situation Down\" NAME=\"action\">");
          }
        }
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"jsessionid\" VALUE=\"" + session.getId() + "\">");
      webPageOutput.println("</FORM>");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      pool.returnConnection(database);
      Routines.WriteHTMLTail(request,response,webPageOutput);
      }

   private void tableHeaders(int type,
                             PrintWriter webPageOutput)
      {
      //Title Line 1
      Routines.tableDataStart(true,true,true,true,false,3,0,"scoresrow",webPageOutput);
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,18,0,"scoresrow",webPageOutput);
      webPageOutput.println("");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,true,true,false,false,12,4,"scoresrow",webPageOutput);
      webPageOutput.println("Downs");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,12,2,"scoresrow",webPageOutput);
      webPageOutput.println("Time");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,false,false,false,3,0,"scoresrow",webPageOutput);
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,12,2,"scoresrow",webPageOutput);
      webPageOutput.println("Down");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,3,0,"scoresrow",webPageOutput);
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,12,2,"scoresrow",webPageOutput);
      webPageOutput.println("Goal");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,3,0,"scoresrow",webPageOutput);
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,12,2,"scoresrow",webPageOutput);
      webPageOutput.println("Points");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,5,0,"scoresrow",webPageOutput);
      webPageOutput.println("Play");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,5,0,"scoresrow",webPageOutput);
      if(type==2)
        {
        webPageOutput.println("Play");
        }
      else
        {
        webPageOutput.println("Call");
        }
      Routines.tableDataEnd(true,false,true,webPageOutput);
      //Title Line 2
      Routines.tableDataStart(true,true,true,true,false,3,0,"scoresrow",webPageOutput);
      webPageOutput.print("Sel");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,18,0,"scoresrow",webPageOutput);
      webPageOutput.println("Name");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,3,0,"scoresrow",webPageOutput);
      webPageOutput.println("1");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,3,0,"scoresrow",webPageOutput);
      webPageOutput.println("2");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,3,0,"scoresrow",webPageOutput);
      webPageOutput.println("3");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,3,0,"scoresrow",webPageOutput);
      webPageOutput.println("4");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,5,0,"scoresrow",webPageOutput);
      webPageOutput.println("From");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,7,0,"scoresrow",webPageOutput);
      webPageOutput.println("To");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,false,false,false,3,0,"scoresrow",webPageOutput);
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,5,0,"scoresrow",webPageOutput);
      webPageOutput.println("From");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,7,0,"scoresrow",webPageOutput);
      webPageOutput.println("To");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,3,0,"scoresrow",webPageOutput);
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,5,0,"scoresrow",webPageOutput);
      webPageOutput.println("From");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,7,0,"scoresrow",webPageOutput);
      webPageOutput.println("To");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,3,0,"scoresrow",webPageOutput);
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,5,0,"scoresrow",webPageOutput);
      webPageOutput.println("From");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,7,0,"scoresrow",webPageOutput);
      webPageOutput.println("To");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,5,0,"scoresrow",webPageOutput);
      webPageOutput.println("Order");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,5,0,"scoresrow",webPageOutput);
      if(type==2)
        {
        webPageOutput.println("Type");
        }
      else
        {
        webPageOutput.println("Against");
        }
      Routines.tableDataEnd(true,false,true,webPageOutput);
      }

   private void formLine(String action,
                         String titleText,
                         String situationName,
                         int type,
                         int down1,
                         int down2,
                         int down3,
                         int down4,
                         int timeFrom,
                         int timeTo,
                         int ydsDownFrom,
                         int ydsDownTo,
                         int ydsScoreFrom,
                         int ydsScoreTo,
                         int pointsFrom,
                         int pointsTo,
                         int playOrder,
                         int[] callNumber,
                         int[] callNumbers,
                         String[] callNames,
                         PrintWriter webPageOutput,
                         Connection  database)
      {
      int maxOrderNumber=0;
      int orderNumbers[]=new int[20];
      String orderNames[]=new String[20];
      String timeText="";
      String yardsText="";
      String scoreText="";
      String selectText="";
      Routines.tableStart(false,webPageOutput);
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT PlayOrderNumber,PlayOrderName " +
                                     "FROM playorders " +
                                     "ORDER BY Sequence ASC");
        while(queryResult.next())
             {
             orderNumbers[maxOrderNumber]=queryResult.getInt(1);
             orderNames[maxOrderNumber]=queryResult.getString(2);
             maxOrderNumber++;
             }
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Unable to find playorder entries : " + error,false,context);
        }
      if("New Situation".equals(action))
        {
        Routines.tableHeader("Enter details of new " + titleText + " situation",18,webPageOutput);
        }
      if("Change Situation".equals(action))
        {
        Routines.tableHeader("Amend details of " + titleText + " situation",18,webPageOutput);
        }
      //Title Line 1
      Routines.tableDataStart(true,true,true,true,false,3,0,"scoresrow",webPageOutput);
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,21,0,"scoresrow",webPageOutput);
      webPageOutput.println("");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,true,true,false,false,12,4,"scoresrow",webPageOutput);
      webPageOutput.println("Downs");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,12,2,"scoresrow",webPageOutput);
      webPageOutput.println("Time");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,52,2,"scoresrow",webPageOutput);
      Routines.tableDataEnd(true,false,true,webPageOutput);
      //Title Line 2
      Routines.tableDataStart(true,true,true,true,false,3,0,"scoresrow",webPageOutput);
      webPageOutput.print("Sel");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,21,0,"scoresrow",webPageOutput);
      webPageOutput.println("Name");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,3,0,"scoresrow",webPageOutput);
      webPageOutput.println("1");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,3,0,"scoresrow",webPageOutput);
      webPageOutput.println("2");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,3,0,"scoresrow",webPageOutput);
      webPageOutput.println("3");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,3,0,"scoresrow",webPageOutput);
      webPageOutput.println("4");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,5,0,"scoresrow",webPageOutput);
      webPageOutput.println("From");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,7,0,"scoresrow",webPageOutput);
      webPageOutput.println("To");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,55,0,"scoresrow",webPageOutput);
      if(type==2)
        {
        webPageOutput.println("Play Type");
        }
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableDataStart(true,true,false,true,false,3,0,"scoresrow",webPageOutput);
      webPageOutput.print("<INPUT TYPE=\"CHECKBOX\" DISABLED NAME=\"dummy\" VALUE=\"true\" CHECKED>");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,false,false,false,false,21,0,"scoresrow",webPageOutput);
      webPageOutput.print("<INPUT TYPE=\"TEXT\" NAME=\"situationName\" SIZE=\"20\" MAXLENGTH=\"20\" VALUE=\"" + situationName + "\">");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,true,false,false,false,3,0,"scoresrow",webPageOutput);
      webPageOutput.print("<INPUT TYPE=\"CHECKBOX\" NAME=\"down1\" VALUE=\"1\"");
      if(down1==1)
        {
        webPageOutput.print(" CHECKED");
        }
      webPageOutput.println(">");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,true,false,false,false,3,0,"scoresrow",webPageOutput);
      webPageOutput.print("<INPUT TYPE=\"CHECKBOX\" NAME=\"down2\" VALUE=\"1\"");
      if(down2==1)
        {
        webPageOutput.print(" CHECKED");
        }
      webPageOutput.println(">");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,true,false,false,false,3,0,"scoresrow",webPageOutput);
      webPageOutput.print("<INPUT TYPE=\"CHECKBOX\" NAME=\"down3\" VALUE=\"1\"");
      if(down3==1)
        {
        webPageOutput.print(" CHECKED");
        }
      webPageOutput.println(">");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,true,false,false,false,3,0,"scoresrow",webPageOutput);
      webPageOutput.print("<INPUT TYPE=\"CHECKBOX\" NAME=\"down4\" VALUE=\"1\"");
      if(down4==1)
        {
        webPageOutput.print(" CHECKED");
        }
      webPageOutput.println(">");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,false,false,false,false,5,0,"scoresrow",webPageOutput);
      webPageOutput.println("<SELECT NAME=\"timeFrom\">");
      for(int currentTime=0;currentTime<76;currentTime++)
         {
         switch(currentTime)
               {
               case 0:
               timeText="Start";
               break;
               case 15:
               timeText="2nd";
               break;
               case 30:
               timeText="Half";
               break;
               case 45:
               timeText="4th";
               break;
               case 75:
               timeText="End";
               break;
               default:
               timeText=String.valueOf(currentTime);
               }
         if(currentTime==timeFrom)
           {
           selectText=" SELECTED";
           }
         else
           {
           selectText="";
           }
         webPageOutput.println(" <OPTION" + selectText + " VALUE=\"" + currentTime + "\">" + timeText);
         }
      webPageOutput.println("</SELECT>");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,false,false,false,false,7,0,"scoresrow",webPageOutput);
      webPageOutput.println("<SELECT NAME=\"timeTo\">");
      for(int currentTime=0;currentTime<76;currentTime++)
         {
         switch(currentTime)
               {
               case 0:
               timeText="Start";
               break;
               case 15:
               timeText="2nd";
               break;
               case 30:
               timeText="Half";
               break;
               case 45:
               timeText="4th";
               break;
               case 75:
               timeText="End";
               break;
               default:
               timeText=String.valueOf(currentTime);
               }
         if(currentTime==timeTo)
           {
           selectText=" SELECTED";
           }
         else
           {
           selectText="";
           }
         webPageOutput.println(" <OPTION" + selectText + " VALUE=\"" + currentTime + "\">" + timeText);
         }
      webPageOutput.println("</SELECT>");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,false,false,false,false,52,0,"scoresrow",webPageOutput);
      if(type==2)
        {
        webPageOutput.println("<SELECT NAME=\"callNumber\">");
        for(int currentCall=0;currentCall<callNames.length;currentCall++)
         {
         if(callNumbers[currentCall]!=0)
           {
           selectText="";
           if(callNumber!=null)
             {
             if(callNumbers[currentCall]==callNumber[0])
               {
               selectText=" SELECTED";
               }
             }
           webPageOutput.println(" <OPTION" + selectText + " VALUE=\"" + callNumbers[currentCall] + "\">" + callNames[currentCall]);
           }
         }
        webPageOutput.println("</SELECT>");
        }
      Routines.tableDataEnd(false,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      Routines.tableStart(false,webPageOutput);
      //Title Line 1
      Routines.tableDataStart(true,true,true,true,false,0,8,"scoresrow",webPageOutput);
      webPageOutput.println(Routines.indent(1));
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,3,0,"scoresrow",webPageOutput);
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,21,2,"scoresrow",webPageOutput);
      webPageOutput.println("Down");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,21,2,"scoresrow",webPageOutput);
      webPageOutput.println("Goal");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,40,2,"scoresrow",webPageOutput);
      webPageOutput.println("Points");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,15,0,"scoresrow",webPageOutput);
      webPageOutput.println("Play");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      //Title Line 2
      Routines.tableDataStart(true,true,true,true,false,3,0,"scoresrow",webPageOutput);
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,11,0,"scoresrow",webPageOutput);
      webPageOutput.println("From");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,10,0,"scoresrow",webPageOutput);
      webPageOutput.println("To");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,11,0,"scoresrow",webPageOutput);
      webPageOutput.println("From");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,10,0,"scoresrow",webPageOutput);
      webPageOutput.println("To");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,20,0,"scoresrow",webPageOutput);
      webPageOutput.println("From");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,20,0,"scoresrow",webPageOutput);
      webPageOutput.println("To");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,15,0,"scoresrow",webPageOutput);
      webPageOutput.println("Order");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,3,0,"scoresrow",webPageOutput);
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,false,false,false,false,5,0,"scoresrow",webPageOutput);
      webPageOutput.println("<SELECT NAME=\"ydsDownFrom\">");
      for(int currentYards=1;currentYards<100;currentYards++)
         {
         switch(currentYards)
               {
               default:
               yardsText=String.valueOf(currentYards) + "yd";
               }
         if(currentYards==ydsDownFrom)
           {
           selectText=" SELECTED";
           }
         else
           {
           selectText="";
           }
         webPageOutput.println(" <OPTION" + selectText + " VALUE=\"" + currentYards + "\">" + yardsText);
         }
      webPageOutput.println("</SELECT>");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,false,false,false,false,7,0,"scoresrow",webPageOutput);
      webPageOutput.println("<SELECT NAME=\"ydsDownTo\">");
      for(int currentYards=1;currentYards<100;currentYards++)
         {
         switch(currentYards)
               {
               case 99:
               yardsText="Any";
               break;
               default:
               yardsText=String.valueOf(currentYards) + "yd";
               }
         if(currentYards==ydsDownTo)
           {
           selectText=" SELECTED";
           }
         else
           {
           selectText="";
           }
         webPageOutput.println(" <OPTION" + selectText + " VALUE=\"" + currentYards + "\">" + yardsText);
         }
      webPageOutput.println("</SELECT>");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,false,false,false,false,5,0,"scoresrow",webPageOutput);
      webPageOutput.println("<SELECT NAME=\"ydsScoreFrom\">");
      for(int currentYards=1;currentYards<100;currentYards++)
         {
         switch(currentYards)
               {
               default:
               yardsText=String.valueOf(currentYards) + "yd";
               }
         if(currentYards==ydsScoreFrom)
           {
           selectText=" SELECTED";
           }
         else
           {
           selectText="";
           }
         webPageOutput.println(" <OPTION" + selectText + " VALUE=\"" + currentYards + "\">" + yardsText);
         }
      webPageOutput.println("</SELECT>");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,false,false,false,false,7,0,"scoresrow",webPageOutput);
      webPageOutput.println("<SELECT NAME=\"ydsScoreTo\">");
      for(int currentYards=1;currentYards<100;currentYards++)
         {
         switch(currentYards)
               {
               case 99:
               yardsText="Any";
               break;
               default:
               yardsText=String.valueOf(currentYards) + "yd";
               }
         if(currentYards==ydsScoreTo)
           {
           selectText=" SELECTED";
           }
         else
           {
           selectText="";
           }
         webPageOutput.println(" <OPTION" + selectText + " VALUE=\"" + currentYards + "\">" + yardsText);
         }
      webPageOutput.println("</SELECT>");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,false,false,false,false,5,0,"scoresrow",webPageOutput);
      webPageOutput.println("<SELECT NAME=\"pointsFrom\">");
      for(int currentScore=-22;currentScore<23;currentScore++)
         {
         if(currentScore==-22)
           {
           scoreText="Down";
           }
         else
           {
           if(currentScore==0)
             {
             scoreText="Tied";
             }
           else
             {
             if(currentScore<0)
               {
               scoreText="Down by " + (currentScore*-1);
               }
             else
               {
               scoreText="Up by " + currentScore;
               }
             }
           }
         if(currentScore==pointsFrom)
           {
           selectText=" SELECTED";
           }
         else
           {
           selectText="";
           }
         webPageOutput.println(" <OPTION" + selectText + " VALUE=\"" + currentScore + "\">" + scoreText);
         }
      webPageOutput.println("</SELECT>");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,false,false,false,false,7,0,"scoresrow",webPageOutput);
      webPageOutput.println("<SELECT NAME=\"pointsTo\">");
      for(int currentScore=-22;currentScore<23;currentScore++)
         {
         if(currentScore==0)
           {
           scoreText="Tied";
           }
         else
           {
           if(currentScore==+22)
             {
             scoreText="Up";
             }
           else
             {
             if(currentScore<0)
               {
               scoreText="Down by " + (currentScore*-1);
               }
             else
               {
               scoreText="Up by " + currentScore;
               }
             }
           }
         if(currentScore==pointsTo)
           {
           selectText=" SELECTED";
           }
         else
           {
           selectText="";
           }
         webPageOutput.println(" <OPTION" + selectText + " VALUE=\"" + currentScore + "\">" + scoreText);
         }
      webPageOutput.println("</SELECT>");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,false,false,false,false,5,0,"scoresrow",webPageOutput);
      webPageOutput.println("<SELECT NAME=\"playOrder\">");
      for(int currentOrder=0;currentOrder<maxOrderNumber;currentOrder++)
         {
         if(orderNumbers[currentOrder]==playOrder)
           {
           selectText=" SELECTED";
           }
         else
           {
           selectText="";
           }
         webPageOutput.println(" <OPTION" + selectText + " VALUE=\"" + orderNumbers[currentOrder] + "\">" + orderNames[currentOrder]);
         }
      webPageOutput.println("</SELECT>");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      if(type!=2)
        {
        //Title Line 3
        Routines.tableStart(false,webPageOutput);
        Routines.tableDataStart(true,true,true,true,false,0,8,"scoresrow",webPageOutput);
        webPageOutput.println(Routines.indent(1));
        Routines.tableDataEnd(true,false,true,webPageOutput);
        Routines.tableDataStart(true,true,true,true,false,0,3,"scoresrow",webPageOutput);
        webPageOutput.println("Call against these conditions");
        Routines.tableDataEnd(true,false,true,webPageOutput);
        int fieldCount=0;
        boolean lineFilled=true;
        for(int currentCall=0;currentCall<callNumbers.length;currentCall++)
           {
           if(callNumbers[currentCall]!=0)
             {
             if(fieldCount==0)
               {
               Routines.tableDataStart(true,false,false,true,false,33,0,"scoresrow",webPageOutput);
               }
             else
               {
               Routines.tableDataStart(true,false,false,false,false,33,0,"scoresrow",webPageOutput);
               }
             webPageOutput.print("<INPUT TYPE=\"CHECKBOX\" NAME=\"" + callNumbers[currentCall] + "\" VALUE=\"1\"");
             if(callNumber!=null)
               {
               for(int selectedCall=0;selectedCall<callNumber.length;selectedCall++)
                  {
                  if(callNumbers[currentCall]==callNumber[selectedCall])
                    {
                    webPageOutput.print(" CHECKED");
                    }
                  }
               }
             webPageOutput.println(">");
             webPageOutput.println(callNames[currentCall]);
             if(fieldCount==2)
               {
               Routines.tableDataEnd(true,false,true,webPageOutput);
               fieldCount=0;
               lineFilled=true;
               }
             else
               {
               Routines.tableDataEnd(false,false,false,webPageOutput);
               fieldCount++;
               }
             }
           }
       if(lineFilled&&fieldCount==1)
         {
         Routines.tableDataStart(true,false,false,false,false,33,0,"scoresrow",webPageOutput);
         Routines.tableDataEnd(false,false,false,webPageOutput);
         Routines.tableDataStart(true,false,false,false,false,33,0,"scoresrow",webPageOutput);
         Routines.tableDataEnd(true,false,true,webPageOutput);
         }
       if(lineFilled&&fieldCount==2)
         {
         Routines.tableDataStart(true,false,false,false,false,33,0,"scoresrow",webPageOutput);
         Routines.tableDataEnd(true,false,true,webPageOutput);
         }
        Routines.tableDataEnd(false,false,true,webPageOutput);
        Routines.tableEnd(webPageOutput);
        }
      }

   private synchronized boolean updateEntry(String action,
                                            int type,
                                            int[] callNumbers,
                                            HttpSession session,
                                            HttpServletRequest request,
                                            Connection database)
      {
      boolean success=false;
      int situationNumber=Routines.safeParseInt(request.getParameter("situationNumber"));
      int down1=Routines.safeParseInt(request.getParameter("down1"));
      int down2=Routines.safeParseInt(request.getParameter("down2"));
      int down3=Routines.safeParseInt(request.getParameter("down3"));
      int down4=Routines.safeParseInt(request.getParameter("down4"));
      int timeFrom=Routines.safeParseInt(request.getParameter("timeFrom"));
      int timeTo=Routines.safeParseInt(request.getParameter("timeTo"));
      int ydsDownFrom=Routines.safeParseInt(request.getParameter("ydsDownFrom"));
      int ydsDownTo=Routines.safeParseInt(request.getParameter("ydsDownTo"));
      int ydsScoreFrom=Routines.safeParseInt(request.getParameter("ydsScoreFrom"));
      int ydsScoreTo=Routines.safeParseInt(request.getParameter("ydsScoreTo"));
      int pointsFrom=Routines.safeParseInt(request.getParameter("pointsFrom"));
      int pointsTo=Routines.safeParseInt(request.getParameter("pointsTo"));
      int playOrderNumber=Routines.safeParseInt(request.getParameter("playOrder"));
      int sequence=0;
      String situationName=request.getParameter("situationName");
      int callSet=0;
      int[] callNumber=new int[20];
      int maxCalls=0;
      if(type==2)
        {
        callNumber[0]=Routines.safeParseInt(request.getParameter("callNumber"));
        }
      else
        {
        for(int currentCall=0;currentCall<callNumbers.length;currentCall++)
           {
           callSet=Routines.safeParseInt(request.getParameter(String.valueOf(callNumbers[currentCall])));
           if(callSet==1)
             {
             callNumber[maxCalls]=callNumbers[currentCall];
             maxCalls++;
             }
           }
         }
      try
        {
        // Get Latest SequenceNumber.
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT Sequence " +
                                     "FROM defaultsituations " +
                                     "WHERE Type=" + type + " " +
                                     "ORDER BY Sequence DESC");
        if(queryResult.first())
          {
          sequence=queryResult.getInt(1);
          }
        if(situationNumber==0)
          {
          //Get latest situationNumber.
          situationNumber=1;
          queryResult=sql.executeQuery("SELECT SituationNumber " +
                                       "FROM defaultsituations " +
                                       "ORDER BY SituationNumber DESC");
          if(queryResult.first())
            {
            situationNumber=queryResult.getInt(1) + 1;
            }
          }
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Unable to retrieve defaultsituations : " + error,false,context);
        }
      if("Move Situation Up".equals(action))
        {
        boolean moveRequested=false;
        try
          {
          int updates=0;
          Statement sql1=database.createStatement();
          Statement sql2=database.createStatement();
          ResultSet queryResult1;
          ResultSet queryResult2;
          queryResult1=sql1.executeQuery("SELECT SituationNumber " +
                                         "FROM defaultsituations " +
                                         "WHERE Type=" + type + " " +
                                         "ORDER BY Sequence ASC");
          while(queryResult1.next())
               {
               situationNumber=queryResult1.getInt(1);
               int currentSequence=0;
               if("true".equals(request.getParameter(String.valueOf(situationNumber))))
                 {
                 if(!moveRequested)
                   {
                   moveRequested=true;
                   }
                 queryResult2=sql2.executeQuery("SELECT Sequence,SituationName FROM defaultsituations " +
                                                "WHERE SituationNumber=" + situationNumber);
                 queryResult2.first();
                 currentSequence=queryResult2.getInt(1);
                 if(currentSequence==1)
                   {
                   session.setAttribute("message",queryResult2.getString(2) + " is already at the top of the situations list");
                   return false;
                   }
                 updates=sql1.executeUpdate("UPDATE defaultsituations " +
                                            "SET Sequence=(Sequence+1),DateTimeStamp='" +
                                            Routines.getDateTime(false) + "' " +
                                            "WHERE Type=" + type + " " +
                                            "AND Sequence=" + (currentSequence-1));
                 if(updates!=1)
                   {
                   Routines.writeToLog(servletName,"defaultsituation not moved (prior), reason unknown",false,context);
                   }
                 updates=sql1.executeUpdate("UPDATE defaultsituations " +
                                            "SET Sequence=(Sequence-1),DateTimeStamp='" +
                                            Routines.getDateTime(false)  + "' " +
                                            "WHERE SituationNumber=" + situationNumber);
                 if(updates!=1)
                   {
                   Routines.writeToLog(servletName,"defaultsituation not moved (current), reason unknown",false,context);
                   }
                 }
               }
          }
        catch(SQLException error)
          {
          Routines.writeToLog(servletName,"Unable to move defaultsituations : " + error,false,context);
          }
        if(moveRequested)
          {
          session.setAttribute("message","Move successfull");
          }
        else
          {
          session.setAttribute("message","No situations selected");
          }
        success=true;
        }
      if("Move Situation Down".equals(action))
        {
        boolean moveRequested=false;
        try
          {
          int updates=0;
          Statement sql1=database.createStatement();
          Statement sql2=database.createStatement();
          ResultSet queryResult1;
          ResultSet queryResult2;
          queryResult1=sql1.executeQuery("SELECT SituationNumber " +
                                         "FROM defaultsituations " +
                                         "WHERE Type=" + type + " " +
                                         "ORDER BY Sequence DESC");
          while(queryResult1.next())
               {
               situationNumber=queryResult1.getInt(1);
               int currentSequence=0;
               if("true".equals(request.getParameter(String.valueOf(situationNumber))))
                 {
                 if(!moveRequested)
                   {
                   moveRequested=true;
                   }
                 queryResult2=sql2.executeQuery("SELECT Sequence,SituationName FROM defaultsituations " +
                                                "WHERE SituationNumber=" + situationNumber);
                 queryResult2.first();
                 currentSequence=queryResult2.getInt(1);
                 if(currentSequence==sequence)
                   {
                   session.setAttribute("message",queryResult2.getString(2) + " is already at the bottom of the situations list");
                   return false;
                   }
                 updates=sql1.executeUpdate("UPDATE defaultsituations " +
                                            "SET Sequence=(Sequence-1),DateTimeStamp='" +
                                            Routines.getDateTime(false) + "' " +
                                            "WHERE Type=" + type + " " +
                                            "AND Sequence=" + (currentSequence+1));
                 if(updates!=1)
                   {
                   Routines.writeToLog(servletName,"defaultsituations not moved (prior), reason unknown",false,context);
                   }
                 updates=sql1.executeUpdate("UPDATE defaultsituations " +
                                            "SET Sequence=(Sequence+1),DateTimeStamp='" +
                                            Routines.getDateTime(false) + "' " +
                                            "WHERE SituationNumber=" + situationNumber);
                 if(updates!=1)
                   {
                   Routines.writeToLog(servletName,"defaultsituations not moved (current), reason unknown",false,context);
                   }
                 }
               }
          }
        catch(SQLException error)
          {
          Routines.writeToLog(servletName,"Unable to move defaultsituations : " + error,false,context);
          }
        if(moveRequested)
          {
          session.setAttribute("message","Move successfull");
          }
        else
          {
          session.setAttribute("message","No situations selected");
          }
        success=true;
        }
      if("Store New Situation".equals(action))
        {
        try
          {
          int updates=0;
          Statement sql=database.createStatement();
          ResultSet queryResult;
          updates=sql.executeUpdate("INSERT INTO defaultsituations (" +
                                    "SituationNumber,SituationName,Type," +
                                    "Sequence,Down1,Down2,Down3,Down4," +
                                    "TimeFrom,TimeTo,YdsDownFrom,YdsDownTo," +
                                    "YdsScoreFrom,YdsScoreTo,PointsFrom,PointsTo," +
                                    "PlayOrderNumber,SituationCallNumber1,SituationCallNumber2," +
                                    "SituationCallNumber3,SituationCallNumber4," +
                                    "SituationCallNumber5,SituationCallNumber6," +
                                    "SituationCallNumber7,SituationCallNumber8," +
                                    "SituationCallNumber9,SituationCallNumber10," +
                                    "SituationCallNumber11,SituationCallNumber12," +
                                    "SituationCallNumber13,SituationCallNumber14," +
                                    "SituationCallNumber15,SituationCallNumber16," +
                                    "SituationCallNumber17,SituationCallNumber18," +
                                    "SituationCallNumber19,SituationCallNumber20," +
                                    "DateTimeStamp) " +
                                    "VALUES (" +
                                    situationNumber + ",\"" +
                                    situationName + "\"," +
                                    type + "," +
                                    (sequence+1) + "," +
                                    down1 + "," +
                                    down2 + "," +
                                    down3 + "," +
                                    down4 + "," +
                                    timeFrom + "," +
                                    timeTo + "," +
                                    ydsDownFrom + "," +
                                    ydsDownTo + "," +
                                    ydsScoreFrom + "," +
                                    ydsScoreTo + "," +
                                    pointsFrom + "," +
                                    pointsTo + "," +
                                    playOrderNumber + "," +
                                    callNumber[0] + "," +
                                    callNumber[1] + "," +
                                    callNumber[2] + "," +
                                    callNumber[3] + "," +
                                    callNumber[4] + "," +
                                    callNumber[5] + "," +
                                    callNumber[6] + "," +
                                    callNumber[7] + "," +
                                    callNumber[8] + "," +
                                    callNumber[9] + "," +
                                    callNumber[10] + "," +
                                    callNumber[11] + "," +
                                    callNumber[12] + "," +
                                    callNumber[13] + "," +
                                    callNumber[14] + "," +
                                    callNumber[15] + "," +
                                    callNumber[16] + "," +
                                    callNumber[17] + "," +
                                    callNumber[18] + "," +
                                    callNumber[19] + ",'" +
                                    Routines.getDateTime(false) + "')");
          if(updates!=1)
            {
            Routines.writeToLog(servletName,"New defaultsituation not created, reason unknown",false,context);
            }
          }
        catch(SQLException error)
          {
          Routines.writeToLog(servletName,"Unable to create defaultsituations : " + error,false,context);
          }
        session.setAttribute("message",situationName + " situation stored successfully");
        success=true;
        }
      if("Store Changed Situation".equals(action))
        {
        try
          {
          int updates=0;
          Statement sql=database.createStatement();
          ResultSet queryResult;
          updates=sql.executeUpdate("UPDATE defaultsituations " +
                                    "SET SituationName='" + situationName + "'," +
                                    "Down1=" + down1 + "," +
                                    "Down2=" + down2 + "," +
                                    "Down3=" + down3 + "," +
                                    "Down4=" + down4 + "," +
                                    "TimeFrom=" + timeFrom + "," +
                                    "TimeTo=" + timeTo + "," +
                                    "YdsDownFrom=" + ydsDownFrom + "," +
                                    "YdsDownTo=" + ydsDownTo + "," +
                                    "YdsScoreFrom=" + ydsScoreFrom + "," +
                                    "YdsScoreTo=" + ydsScoreTo + "," +
                                    "PointsFrom=" + pointsFrom + "," +
                                    "PointsTo=" + pointsTo + "," +
                                    "PlayOrderNumber=" + playOrderNumber + "," +
                                    "SituationCallNumber1=" + callNumber[0] + "," +
                                    "SituationCallNumber2=" + callNumber[1] + "," +
                                    "SituationCallNumber3=" + callNumber[2] + "," +
                                    "SituationCallNumber4=" + callNumber[3] + "," +
                                    "SituationCallNumber5=" + callNumber[4] + "," +
                                    "SituationCallNumber6=" + callNumber[5] + "," +
                                    "SituationCallNumber7=" + callNumber[6] + "," +
                                    "SituationCallNumber8=" + callNumber[7] + "," +
                                    "SituationCallNumber9=" + callNumber[8] + "," +
                                    "SituationCallNumber10=" + callNumber[9] + "," +
                                    "SituationCallNumber11=" + callNumber[10] + "," +
                                    "SituationCallNumber12=" + callNumber[11] + "," +
                                    "SituationCallNumber13=" + callNumber[12] + "," +
                                    "SituationCallNumber14=" + callNumber[13] + "," +
                                    "SituationCallNumber15=" + callNumber[14] + "," +
                                    "SituationCallNumber16=" + callNumber[15] + "," +
                                    "SituationCallNumber17=" + callNumber[16] + "," +
                                    "SituationCallNumber18=" + callNumber[17] + "," +
                                    "SituationCallNumber19=" + callNumber[18] + "," +
                                    "SituationCallNumber20=" + callNumber[19] + "," +
                                    "DateTimeStamp='" +
                                    Routines.getDateTime(false) + "' " +
                                    "WHERE SituationNumber=" + situationNumber);
          if(updates!=1)
            {
            Routines.writeToLog(servletName,"defaultsituations not updated, reason unknown",false,context);
            }
          }
        catch(SQLException error)
          {
          Routines.writeToLog(servletName,"Unable to update defaultsituations : " + error,false,context);
          }
        session.setAttribute("message",situationName + " situation changed successfully");
        success=true;
        }
      if("Delete Situation".equals(action))
        {
        boolean deleteRequested=false;
        try
          {
          int updates=0;
          Statement sql1=database.createStatement();
          Statement sql2=database.createStatement();
          ResultSet queryResult1;
          ResultSet queryResult2;
          queryResult1=sql1.executeQuery("SELECT SituationNumber " +
                                         "FROM defaultsituations " +
                                         "WHERE Type=" + type);
          while(queryResult1.next())
               {
               situationNumber=queryResult1.getInt(1);
               if("true".equals(request.getParameter(String.valueOf(situationNumber))))
                 {
                 if(!deleteRequested)
                   {
                   deleteRequested=true;
                   }
                 queryResult2=sql2.executeQuery("SELECT PlayBookNumber " +
                                                "FROM defaultplaybook " +
                                                "WHERE SituationNumber=" + situationNumber);
                 if(queryResult2.first())
                   {
                   session.setAttribute("message","Situation currently in use by defaultplaybook entries");
                   return false;
                   }
                 else
                   {
                   queryResult2=sql2.executeQuery("SELECT PlayBookNumber " +
                                                  "FROM playbook " +
                                                  "WHERE SituationNumber=" + situationNumber);
                   if(queryResult2.first())
                     {
                     session.setAttribute("message","Situation currently in use by playbook entries");
                     return false;
                     }
                   else
                     {
                     updates=sql2.executeUpdate("DELETE FROM defaultsituations " +
                                                "WHERE SituationNumber=" + situationNumber);
                     if(updates!=1)
                       {
                       Routines.writeToLog(servletName,"defaultsituation not deleted (" + situationNumber + ")",false,context);
                       }
                     }
                   }
                 }
               }
          queryResult1=sql1.executeQuery("SELECT SituationNumber " +
                                         "FROM defaultsituations " +
                                         "WHERE Type=" + type + " " +
                                         "ORDER BY Sequence ASC");
          int newSequence=0;
          situationNumber=0;
          while(queryResult1.next())
                {
                newSequence++;
                situationNumber=queryResult1.getInt(1);
                updates=sql2.executeUpdate("UPDATE defaultsituations " +
                                           "SET Sequence=" + newSequence + ",DateTimeStamp='" +
                                           Routines.getDateTime(false) + "' " +
                                           "WHERE SituationNumber=" + situationNumber);
                 if(updates!=1)
                   {
                   Routines.writeToLog(servletName,"Situation entry not reset (" + situationNumber + ")",false,context);
                   }
                }
          }
        catch(SQLException error)
          {
          Routines.writeToLog(servletName,"Unable to delete defaultsituations : " + error,false,context);
          }
        if(deleteRequested)
          {
          session.setAttribute("message","Delete successfull");
          }
        else
          {
          session.setAttribute("message","No situations selected");
          }
        success=true;
        }
      return success;
      }
}