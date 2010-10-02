import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;

public class GameRunner extends HttpServlet implements Runnable
   {
   private ServletContext context;
   private int fixtureNumber=0;
   private int leagueNumber=0;
   private static String servletName="GameRunner";
   private static String lock="";
   private Properties properties;
   private boolean active=true;
   private boolean endOfGame=false;
   private boolean postSeason=false;
   private boolean changeOfPossession=false;
   private int timeGone=0;
   private int homeTeamScore=0;
   private int awayTeamScore=0;
   private static int sizeOfSituationsArray=35+1;
   private static int sizeOfPlaysArray=16;
   private static int sizeOfCardArray=92;
   private static int sizeOfFormationsArray=16;
   private static int sizeOfDepthChartsArray=3;
   private static int sizeOfPlayersArray=55;
   private static int sizeOfSkillsArray=14;
   private static int sizeOfPositionSkillsArray=3;
   private static int sizeOfInjuriesArray=2;
   private static int sizeOfCoverageArray=22;
   private static int startOfSkills=13;
   private static int zValidFor=0;
   private static int zValidForSettingNumber=1;
   private static int numOfZPerPlay=0;
   private static int numOfZPerPlaySettingNumber=2;
   private static int numOfDepthChartPositions=0;
   private static int[][] formations=null;
   private static int[][] skills=null;
   private static int[][] positionSkills=null;
   private static int[][] injuries=null;
   private static int[][] positionDepthCharts=null;
   private static int[] onsideLeftRecoverers={0,1,4,5};
   private static int[] onsideRightRecoverers={2,3,6,5};
   private ConnectionPool pool;
      
   GameRunner(ServletContext context,int fixtureNumber,int leagueNumber,boolean postSeason)
	  {
	  this.context=context;
	  this.fixtureNumber=fixtureNumber;
	  this.leagueNumber=leagueNumber;
	  this.postSeason=postSeason;
	  }

   public void run()
	  {
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
		Connection database=null;
		try
		  {
		  database=pool.getConnection(servletName);
		  }
		catch(SQLException error)
		  {
		  Routines.writeToLog(servletName,"Unable to connect to database : " + error,false,context);
		  }		
	  System.out.println("GameRunner for fixture " + fixtureNumber + " reporting for duty");
	  boolean advanced=false;
	  try
		{
		Statement sql=database.createStatement();
		ResultSet queryResult=sql.executeQuery("SELECT LeagueLevel " +
											   "FROM fixtures,leagues " +
											   "WHERE FixtureNumber=" + fixtureNumber + " " +
											   "AND fixtures.LeagueNumber=leagues.LeagueNumber");
		if(queryResult.first())
		  {
		  if(queryResult.getInt(1)>2)
		    {
		    advanced=true;	
		    }
		  }
		}  
	 catch(SQLException error)
		{
		Routines.writeToLog(servletName,"Error getting team names : " + error,false,context);
		}	
	 //Get zValidFor	  	
	 try
	   {
	   Statement sql=database.createStatement();
	   ResultSet queryResult=sql.executeQuery("SELECT Value " +
											  "FROM gamesettings " +
											  "WHERE SettingNumber=" + zValidForSettingNumber);
	   if(queryResult.first())
		 {
		 zValidFor=queryResult.getInt(1);
		 }
	   }  
	  catch(SQLException error)
	   {
	   Routines.writeToLog(servletName,"Error getting gamesettings(zValidFor) : " + error,false,context);
	   }
	 //Get numOfZPerPlay	  	
	 try
	   {
	   Statement sql=database.createStatement();
	   ResultSet queryResult=sql.executeQuery("SELECT Value " +
											  "FROM gamesettings " +
											  "WHERE SettingNumber=" + numOfZPerPlaySettingNumber);
	   if(queryResult.first())
		 {
		 numOfZPerPlay=queryResult.getInt(1);
		 }
	   }  
	  catch(SQLException error)
	   {
	   Routines.writeToLog(servletName,"Error getting gamesettings(numOfZPerPlay) : " + error,false,context);
	   }	   
	  int homeTeam=0;
	  int awayTeam=0;
	  try
		{
		Statement sql=database.createStatement();
		ResultSet queryResult=sql.executeQuery("SELECT HomeTeam,AwayTeam " +
									           "FROM fixtures " +
									           "WHERE FixtureNumber=" + fixtureNumber);
		if(queryResult.first())
		  {
		  homeTeam=queryResult.getInt(1);
		  awayTeam=queryResult.getInt(2);	
		  }
		}  
	 catch(SQLException error)
		{
		Routines.writeToLog(servletName,"Error getting team names : " + error,false,context);
		}
	 //Load Situations	
	 int[][] homeSituations=null;
	 int[][] awaySituations=null;	
	 for(int currentTeam=0;currentTeam<2;currentTeam++)
	    {
	    int tempTeam=0;
	    if(currentTeam==0)
	      {
	      tempTeam=homeTeam;
	      }
	    else
	      {  
	      tempTeam=awayTeam;	
	      }
	    try
	      {
	      String tableText="";
	      String whereText="";
	      if(advanced)
	        {
	        tableText="situations ";
	        whereText="WHERE TeamNumber=" + tempTeam + " ";	
	        }
	      else
	        {
	        tableText="defaultsituations ";	  
	        }
	      Statement sql=database.createStatement();
		  ResultSet queryResult=sql.executeQuery("SELECT COUNT(SituationNumber) " +
									             "FROM " + tableText +
									             whereText);
	      if(queryResult.first())
		    {
		    int numOfSituations=queryResult.getInt(1);
		    if(currentTeam==0)
		      {
		      homeSituations=new int[numOfSituations][sizeOfSituationsArray];	
		      }
		    else
		      {
			  awaySituations=new int[numOfSituations][sizeOfSituationsArray];	  
		      }
		    }
		  queryResult=sql.executeQuery("SELECT SituationNumber,Type,Down1,Down2,Down3,Down4," +
		                               "TimeFrom,TimeTo,YdsDownFrom,YdsDownTo," +
		                               "YdsScoreFrom,YdsScoreTo,PointsFrom,PointsTo," +
		                               "PlayOrderNumber," +
		                               "SituationCallNumber1,SituationCallNumber2,SituationCallNumber3,SituationCallNumber4,SituationCallNumber5," +
			                           "SituationCallNumber6,SituationCallNumber7,SituationCallNumber8,SituationCallNumber9,SituationCallNumber10," +
			                           "SituationCallNumber11,SituationCallNumber12,SituationCallNumber13,SituationCallNumber14,SituationCallNumber15," +
			                           "SituationCallNumber16,SituationCallNumber17,SituationCallNumber18,SituationCallNumber19,SituationCallNumber20 " +
									   "FROM " + tableText +
									   whereText +
									   "ORDER BY Type DESC, Sequence ASC");
     	  int currentSituation=0;
		  while(queryResult.next())
		     {
		     for(int currentItem=0;currentItem<(sizeOfSituationsArray-1);currentItem++)
		        {
				if(currentTeam==0)
				  {
				  homeSituations[currentSituation][currentItem]=queryResult.getInt(currentItem+1);	
				  }
				else
				  {
				  awaySituations[currentSituation][currentItem]=queryResult.getInt(currentItem+1);	  
				  }		        									   		    
		        }   
		     currentSituation++;   		        
		     }
	      }  
	   catch(SQLException error)
	      {
	      Routines.writeToLog(servletName,"Error getting situations : " + error,false,context);
	      }
	   }   
	   for(int currentElement=0;currentElement<homeSituations.length;currentElement++)
		  {
		  homeSituations[currentElement][sizeOfSituationsArray-1]=-999;	
		  }
	   for(int currentElement=0;currentElement<awaySituations.length;currentElement++)
		  {
		  awaySituations[currentElement][sizeOfSituationsArray-1]=-999;	
		  }		  			      	    
	    //Load PlayBook
		int[][] homePlayBook=null;
		int[][] awayPlayBook=null;
		for(int currentTeam=0;currentTeam<2;currentTeam++)
		   {
		   int tempTeam=0;
		   if(currentTeam==0)
			 {
			 tempTeam=homeTeam;
			 }
		   else
			 {  
			 tempTeam=awayTeam;	
			 }
		   try
			 {
			 Statement sql=database.createStatement();
			 ResultSet queryResult=sql.executeQuery("SELECT COUNT(PlayBookNumber) " +
													"FROM playbook " +
													"WHERE TeamNumber=" + tempTeam);
			 if(queryResult.first())
			   {
			   int numOfPlayBookEntries=queryResult.getInt(1);
			   if(currentTeam==0)
				 {
				 homePlayBook=new int[numOfPlayBookEntries][sizeOfPlaysArray];	
				 }
			   else
				 {
				 awayPlayBook=new int[numOfPlayBookEntries][sizeOfPlaysArray];	  
				 }
			   }
			 queryResult=sql.executeQuery("SELECT SituationNumber,SetNumber,FormationNumber,PositionNumber,playbook.PlayNumber,PrimaryStrategyNumber," +
			                              "SecondaryStrategyNumber1,SecondaryStrategyNumber2,SecondaryStrategyNumber3,SecondaryStrategyNumber4,SecondaryStrategyNumber5," +
				                          "SecondaryStrategyNumber6,SecondaryStrategyNumber7,SecondaryStrategyNumber8,SecondaryStrategyNumber9,SecondaryStrategyNumber10 " +
										  "FROM playbook,plays " +
										  "WHERE TeamNumber=" + tempTeam + " " +
										  "AND playbook.PlayNumber=plays.PlayNumber " +
										  "ORDER BY SituationNumber ASC, playbook.Sequence ASC");
			 int currentPlayBookEntry=0;
			 while(queryResult.next())
				{
				for(int currentItem=0;currentItem<sizeOfPlaysArray;currentItem++)
				   {
				   if(currentTeam==0)
					 {
					 homePlayBook[currentPlayBookEntry][currentItem]=queryResult.getInt(currentItem+1);	
					 }
				   else
					 {
					 awayPlayBook[currentPlayBookEntry][currentItem]=queryResult.getInt(currentItem+1);	  
					 }		        									   		    
				   }
 			    currentPlayBookEntry++;   		        
				}
			 }  
		  catch(SQLException error)
			 {
			 Routines.writeToLog(servletName,"Error getting playbooks : " + error,false,context);
			 }
		   }   	    
		//Load DepthCharts
		int[][] homeDepthChart=null;
		int[][] awayDepthChart=null;	
		for(int currentTeam=0;currentTeam<2;currentTeam++)
		   {
		   int tempTeam=0;
		   if(currentTeam==0)
			 {
			 tempTeam=homeTeam;
			 }
		   else
			 {  
			 tempTeam=awayTeam;	
			 }
		   try
			 {
			 Statement sql=database.createStatement();
			 ResultSet queryResult=sql.executeQuery("SELECT COUNT(DepthChartNumber) " +
													"FROM depthcharts,players " +
													"WHERE depthcharts.TeamNumber=" + tempTeam + " " +
				                                    "AND depthcharts.PlayerNumber=players.PlayerNumber " +
				                                    "AND Injury=0");
			 if(queryResult.first())
			   {
			   int numOfDepthChartEntries=queryResult.getInt(1);
			   if(currentTeam==0)
				 {
				 homeDepthChart=new int[numOfDepthChartEntries][sizeOfDepthChartsArray];	
				 }
			   else
				 {
				 awayDepthChart=new int[numOfDepthChartEntries][sizeOfDepthChartsArray];	  
				 }
			   }
			 queryResult=sql.executeQuery("SELECT SetNumber,depthcharts.PositionNumber,depthcharts.PlayerNumber " +
										  "FROM depthcharts,players " +
										  "WHERE depthcharts.TeamNumber=" + tempTeam + " " +
										  "AND depthcharts.PlayerNumber=players.PlayerNumber " +
				                          "AND Injury=0 " +
										  "ORDER BY SetNumber ASC, PositionNumber ASC,Depth ASC");
			 int currentDepthChartEntry=0;
			 while(queryResult.next())
				{
				for(int currentItem=0;currentItem<sizeOfDepthChartsArray;currentItem++)
				   {
				   if(currentTeam==0)
					 {
					 homeDepthChart[currentDepthChartEntry][currentItem]=queryResult.getInt(currentItem+1);	
					 }
				   else
					 {
					 awayDepthChart[currentDepthChartEntry][currentItem]=queryResult.getInt(currentItem+1);	  
					 }		        									   		    
				   } 
				currentDepthChartEntry++;   		        
				}
			 }  
		  catch(SQLException error)
			 {
			 Routines.writeToLog(servletName,"Error getting depthcharts : " + error,false,context);
			 }	 
		   }   	
		//Load Players
		int[][] homePlayers=null;
		int[][] awayPlayers=null;	
		for(int currentTeam=0;currentTeam<2;currentTeam++)
		   {
		   int tempTeam=0;
		   if(currentTeam==0)
			 {
			 tempTeam=homeTeam;
			 }
		   else
			 {  
			 tempTeam=awayTeam;	
			 }
		   try
			 {
			 Statement sql=database.createStatement();
			 ResultSet queryResult=sql.executeQuery("SELECT COUNT(PlayerNumber) " +
													"FROM players " +
													"WHERE TeamNumber=" + tempTeam + " " +
													"AND Injury=0");
			 if(queryResult.first())
			   {
			   int numOfPlayers=queryResult.getInt(1);
			   if(currentTeam==0)
				 {
				 homePlayers=new int[numOfPlayers][sizeOfPlayersArray];	
				 }
			   else
				 {
				 awayPlayers=new int[numOfPlayers][sizeOfPlayersArray];	  
				 }
			   }
			 queryResult=sql.executeQuery("SELECT players.PlayerNumber,PositionNumber,CollegeNumber,'0',Experience,Number," +
			                              "'0','0','0','0'," +
				                          "'0','0','0','0'," +
				                          "SUM(Skill1+Form1),SUM(Skill2+Form2),SUM(Skill3+Form3),SUM(Skill4+Form4),SUM(Skill5+Form5)," +
				                          "SUM(Skill6+Form6),SUM(Skill7+Form7),SUM(Skill8+Form8),SUM(Skill9+Form9),SUM(SKill10+Form10)," +
				                          "SUM(Skill11+Form11),SUM(Skill12+Form12),SUM(Skill13+Form13),SUM(Skill14+Form14),SUM(Skill15+Form15)," +
				                          "SUM(Skill16+Form16),SUM(Skill17+Form17),SUM(Skill18+Form18),SUM(Skill19+Form19),SUM(Skill20+Form20)," +
				                          "SUM(Skill1+Form1),SUM(Skill2+Form2),SUM(Skill3+Form3),SUM(Skill4+Form4),SUM(Skill5+Form5)," +
				                          "SUM(Skill6+Form6),SUM(Skill7+Form7),SUM(Skill8+Form8),SUM(Skill9+Form9),SUM(SKill10+Form10)," +
				                          "SUM(Skill11+Form11),SUM(Skill12+Form12),SUM(Skill13+Form13),SUM(Skill14+Form14),SUM(Skill15+Form15)," +
				                          "SUM(Skill16+Form16),SUM(Skill17+Form17),SUM(Skill18+Form18),SUM(Skill19+Form19),SUM(Skill20+Form20)," +
				                          "Injury " +
										  "FROM players,masterplayers " +
										  "WHERE TeamNumber=" + tempTeam + " " +
										  "AND players.MasterPlayerNumber=masterPlayers.MasterPlayerNumber " +
										  "AND Injury=0 " +
				                          "GROUP BY PlayerNumber " +
										  "ORDER BY PositionNumber ASC, PlayerNumber ASC");
			 int currentPlayer=0;
			 while(queryResult.next())
				{
				for(int currentItem=0;currentItem<sizeOfPlayersArray;currentItem++)
				   {
				   if(currentTeam==0)
					 {
					 homePlayers[currentPlayer][currentItem]=queryResult.getInt(currentItem+1);	
					 }
				   else
					 {
					 awayPlayers[currentPlayer][currentItem]=queryResult.getInt(currentItem+1);	  
					 }		        									   		    
				   } 
				currentPlayer++;   		        
				}
			 }  
		  catch(SQLException error)
			 {
			 Routines.writeToLog(servletName,"Error getting players : " + error,false,context);
			 }	 
		   }   	    
		//Load CoverageTables
		int[] homeCoverageTable=new int[sizeOfCoverageArray];
		int[] awayCoverageTable=new int[sizeOfCoverageArray];	
		for(int currentTeam=0;currentTeam<2;currentTeam++)
		   {
		   int tempTeam=0;
		   if(currentTeam==0)
			 {
			 tempTeam=homeTeam;
			 }
		   else
			 {  
			 tempTeam=awayTeam;	
			 }
		   try
			 {
			 Statement sql=database.createStatement();
			 ResultSet queryResult=sql.executeQuery("SELECT KeyRunner,KeyReceiver," +
			                                        "Receiver01,Receiver02,Receiver03,Receiver04,Receiver05," +
				                                    "Receiver06,Receiver07,Receiver08,Receiver09,Receiver10," +
				                                    "Receiver11,Receiver12,Receiver13,Receiver14,Receiver15," +
				                                    "Receiver16,Receiver17,Receiver18,Receiver19,Receiver20 " +
													"FROM gameboard " +
													"WHERE TeamNumber=" + tempTeam);
			 while(queryResult.next())
				{
				for(int currentItem=0;currentItem<22;currentItem++)
				   {
				   if(currentTeam==0)
					 {
					 homeCoverageTable[currentItem]=queryResult.getInt(currentItem+1);	
					 }
				   else
					 {
					 awayCoverageTable[currentItem]=queryResult.getInt(currentItem+1);	  
					 }		        									   		    
				   } 
				}
             //Amend coverage tables to use pointers to players table rather than playerNumber.
             for(int currentPlayer=0;currentPlayer<sizeOfCoverageArray;currentPlayer++)
                {
                if((currentTeam==0&&homeCoverageTable[currentPlayer]!=0)||(currentTeam==1&&awayCoverageTable[currentPlayer]!=0))
                  {
                  boolean found=false;
                  for(int currentRosterSpot=0;currentRosterSpot<sizeOfPlayersArray;currentRosterSpot++)
                     {
                     if(currentTeam==0)
                       {
                       if(awayPlayers[currentRosterSpot][0]==homeCoverageTable[currentPlayer])
                         {
                         homeCoverageTable[currentPlayer]=currentRosterSpot;
                         found=true;				
                         }
                       }
                     else
                       {  
					   if(homePlayers[currentRosterSpot][0]==awayCoverageTable[currentPlayer])
					     {
					     awayCoverageTable[currentPlayer]=currentRosterSpot;	
					     found=true;			
					     }
                       }
                     }
				  if(!found)
				    {
					Routines.writeToLog(servletName,"Unable to pointer coverageTable for FixtureNumber " + fixtureNumber,false,context);	   
				    }
                  }
                else
                  {    
				  if(currentTeam==0)
					{
				    homeCoverageTable[currentPlayer]=-1;
				    }
				  else
					{  
					awayCoverageTable[currentPlayer]=-1;	
					}
                  }
                }
			 }  
		  catch(SQLException error)
			 {
			 Routines.writeToLog(servletName,"Error getting gameboard : " + error,false,context);
			 }	 
		   }   	    		   		   		   
		//Load Formations
		synchronized(lock)
		   {
		   if(formations==null)
		     {	
		     try
		       {
		       Statement sql=database.createStatement();
		       ResultSet queryResult=sql.executeQuery("SELECT COUNT(FormationNumber) " +
			   									      "FROM formations ");
		       if(queryResult.first())
		         {
			     int numOfFormations=queryResult.getInt(1);
 		         formations=new int[numOfFormations][sizeOfFormationsArray];	
		         queryResult=sql.executeQuery("SELECT FormationNumber," +
										      "Position1,Position2,Position3,Position4,Position5," +
										      "Position6,Position7,Position8,Position9,Position10," +
										      "Position11,Position12,Position13,Position14,Position15 " +
										      "FROM formations " +
										      "ORDER BY Type ASC, SubType ASC, Sequence ASC");
			     int currentFormation=0;
			     while(queryResult.next())
				     {
				     for(int currentItem=0;currentItem<sizeOfFormationsArray;currentItem++)
				        {
				        formations[currentFormation][currentItem]=queryResult.getInt(currentItem+1);	
                        } 
				     currentFormation++;   		        
				     }
		          }	
		        }  
		     catch(SQLException error)
		        {
		        Routines.writeToLog(servletName,"Error getting formations : " + error,false,context);
		        }
		     }
		   }   
		//Load PositionDepthCharts
		synchronized(lock)
		   {
		   if(positionDepthCharts==null)
			 {
			 int numOfPositions=0;		
			 try
			   {
			   Statement sql=database.createStatement();
			   ResultSet queryResult=sql.executeQuery("SELECT MAX(PositionNumber) " +
													  "FROM positions ");
			   if(queryResult.first())
				 {
				 numOfPositions=queryResult.getInt(1)+1;
				 }
			   queryResult=sql.executeQuery("SELECT COUNT(PositionNumber) " +
											"FROM positions " +
											"WHERE (RealPosition=1 OR PositionNumber=16) AND Type!=3");
			   if(queryResult.first())
				 {
				 numOfDepthChartPositions=queryResult.getInt(1);
				 }
			   positionDepthCharts=new int[numOfPositions+1][numOfDepthChartPositions];	 
			   queryResult=sql.executeQuery("SELECT PositionNumber,DepthChartPosition,Sequence " +
											"FROM positiondepthcharts " +
											"ORDER BY PositionNumber ASC, Sequence ASC");
			   while(queryResult.next())
			      {
			      int currentPositionNumber=queryResult.getInt(1);
			      int currentDepthChartPositionNumber=queryResult.getInt(2);
			      int sequence=queryResult.getInt(3);
			      positionDepthCharts[currentPositionNumber][sequence-1]=currentDepthChartPositionNumber;									
			      }
 			   }  
			 catch(SQLException error)
				{
				Routines.writeToLog(servletName,"Error getting positiondepthchart : " + error,false,context);
				}
			 }
		   }   		   
		//Load Skills
		synchronized(lock)
		   {
		   if(skills==null)
			 {	
			 try
			   {
			   Statement sql=database.createStatement();
			   ResultSet queryResult=sql.executeQuery("SELECT COUNT(SkillNumber) " +
													  "FROM skilltables ");
			   if(queryResult.first())
				 {
				 int numOfSkills=queryResult.getInt(1);
				 skills=new int[numOfSkills][sizeOfSkillsArray];	
				 queryResult=sql.executeQuery("SELECT SkillNumber,Cost," +
											  "Value1,Value2,Value3,Value4," +
											  "Value5,Value6,Value7,Value8," +
											  "Value9,Value10,Value11,Value12 " +
											  "FROM skilltables " +
											  "ORDER BY SkillNumber ASC,Cost DESC");
				 int currentSkill=0;
				 while(queryResult.next())
					 {
					 for(int currentItem=0;currentItem<sizeOfSkillsArray;currentItem++)
						{
						skills[currentSkill][currentItem]=queryResult.getInt(currentItem+1);	
						} 
					 currentSkill++;   		        
					 }
				  }	
				}  
			 catch(SQLException error)
				{
				Routines.writeToLog(servletName,"Error getting formations : " + error,false,context);
				}
			 }
		   }  
		//Load PositionSkills
		synchronized(lock)
		   {
		   if(positionSkills==null)
			 {	
			 try
			   {
			   Statement sql=database.createStatement();
			   ResultSet queryResult=sql.executeQuery("SELECT COUNT(PositionSkillNumber) " +
													  "FROM positionskills ");
			   if(queryResult.first())
				 {
				 int numOfPositionSkills=queryResult.getInt(1);
				 positionSkills=new int[numOfPositionSkills][sizeOfPositionSkillsArray];	
				 queryResult=sql.executeQuery("SELECT PositionNumber,SkillNumber,Sequence " +
											  "FROM positionskills " +
											  "ORDER BY SkillNumber ASC,Sequence ASC");
				 int currentPositionSkill=0;
				 while(queryResult.next())
					 {
					 for(int currentItem=0;currentItem<sizeOfPositionSkillsArray;currentItem++)
						{
						positionSkills[currentPositionSkill][currentItem]=queryResult.getInt(currentItem+1);	
						} 
					 currentPositionSkill++;   		        
					 }
				  }	
				}  
			 catch(SQLException error)
				{
				Routines.writeToLog(servletName,"Error getting formations : " + error,false,context);
				}
			 }
		   } 		    		   
		//Load ActionCards
		int[][] actionCards=null;
		try
		  {
		  Statement sql=database.createStatement();
		  ResultSet queryResult=sql.executeQuery("SELECT COUNT(CardNumber) " +
												 "FROM actioncards ");
		  if(queryResult.first())
			{
			int numOfActions=queryResult.getInt(1);
			actionCards=new int[numOfActions][sizeOfCardArray];	
			queryResult=sql.executeQuery("SELECT Normal,RunNumber,OutOfBounds,PassNumber," +
										 "SweepLeftBreak,SweepLeftLE,SweepLeftLT,SweepLeftLG,SweepLeftCN,SweepLeftRG,SweepLeftRT,SweepLeftRE," +										 "SweepLeftBK,SweepLeftLDE,SweepLeftLDT,SweepLeftNT,SweepLeftRDT,SweepLeftRDE,SweepLeftLOLB,SweepLeftLILB,SweepLeftMLB,SweepLeftRILB,SweepLeftROLB," +
				                         "InsideLeftBreak,InsideLeftLE,InsideLeftLT,InsideLeftLG,InsideLeftCN,InsideLeftRG,InsideLeftRT,InsideLeftRE," +
				                         "InsideLeftBK,InsideLeftLDE,InsideLeftLDT,InsideLeftNT,InsideLeftRDT,InsideLeftRDE,InsideLeftLOLB,InsideLeftLILB,InsideLeftMLB,InsideLeftLILB,InsideLeftLOLB," +
				                         "SweepRightBreak,SweepRightLE,SweepRightLT,SweepRightLG,SweepRightCN,SweepRightRG,SweepRightRT,SweepRightRE," +
				                         "SweepRightBK,SweepRightLDE,SweepRightLDT,SweepRightNT,SweepRightRDT,SweepRightRDE,SweepRightLOLB,SweepRightLILB,SweepRightMLB,SweepRightRILB,SweepRightROLB," +
				                         "InsideRightBreak,InsideRightLE,InsideRightLT,InsideRightLG,InsideRightCN,InsideRightRG,InsideRightRT,InsideRightRE," +
				                         "InsideRightBK,InsideRightLDE,InsideRightLDT,InsideRightNT,InsideRightRDT,InsideRightRDE,InsideRightLOLB,InsideRightLILB,InsideRightMLB,InsideRightRILB,InsideRightROLB," +
										 "EndAround,QuickPass,ShortPass,LongPass,Screen,Injury,Fumble,RunQuickPenalty,ShortLongPenalty,PuntPenalty,KickOffPenalty " +
										 "FROM actioncards " +
										 "ORDER BY CardNumber ASC");
			 int currentActionCard=0;
			 while(queryResult.next())
				{
				for(int currentItem=0;currentItem<(sizeOfCardArray-1);currentItem++)
				   {
				   actionCards[currentActionCard][currentItem+1]=queryResult.getInt(currentItem+1);	
				   } 
				currentActionCard++;   		        
				}
			 }	
		   }  
		catch(SQLException error)
		   {
		   Routines.writeToLog(servletName,"Error getting actioncards : " + error,false,context);
		   }		   
		//Load Injuries
		synchronized(lock)
		   {
		   if(injuries==null)
			 {	
			 try
			   {
			   Statement sql=database.createStatement();
			   ResultSet queryResult=sql.executeQuery("SELECT COUNT(InjuryNumber) " +
													  "FROM injuries ");
			   if(queryResult.first())
				 {
				 int numOfInjuries=queryResult.getInt(1);
				 injuries=new int[numOfInjuries][sizeOfInjuriesArray];	
				 queryResult=sql.executeQuery("SELECT PassNumber,Length " +
											  "FROM injuries " +
											  "ORDER BY PassNumber DESC");
				 int currentInjury=0;
				 while(queryResult.next())
					 {
					 for(int currentItem=0;currentItem<sizeOfInjuriesArray;currentItem++)
						{
						injuries[currentInjury][currentItem]=queryResult.getInt(currentItem+1);	
						} 
						currentInjury++;   		        
					 }
				  }	
				}  
			 catch(SQLException error)
				{
				Routines.writeToLog(servletName,"Error getting injuries : " + error,false,context);
				}
			 Routines.writeToLog(servletName,"["+fixtureNumber+"]All data tables loaded ",false,context);	
			 }
		   } 	
	  //PlayGame   
	  Routines.writeToLog(servletName,"["+fixtureNumber+"]Starting to play game",false,context);	 
      boolean kickOffRequired=true;
      boolean extraPointRequired=false;
      boolean homeTeamInPossession=false;
	  int[][] shuffledActionCards=null;
	  int cardData[]=new int[4];
	  cardData[0]=0;
	  int tacklers[]=new int[4];
	  Random random=new Random(fixtureNumber*050475);
	  playObject playData=new playObject();
	  playData=shufflerTron(actionCards,random,playData);	
	  shuffledActionCards=playData.getShuffledActionCards();
	  int down=0;
	  int distance=0;
	  int ballOn=70;
	  int maxYards=0;
	  //testcode
	  int testCounter=0;
	  //end of text code
//	  cardData[0]//currentCard
//	  cardData[2]//totalNumOfZCards
      while(!endOfGame&&testCounter<200)
         {
		 cardData[1]=0;//numOfZCards
         cardData[3]=0;//numOfCardsThisPlay
         playData.setCardData(cardData);
         maxYards=ballOn;
         boolean fumble=false;
		 int specialCards=0;
         playData=nextCard(playData,actionCards,random);
	     shuffledActionCards=playData.getShuffledActionCards();
         cardData=playData.getCardData();	
		 int homeSituation=-1;	
		 int awaySituation=-1;	
		 int homePlay=-1;
		 int awayPlay=-1;
		 int homeFormation=-1;
		 int awayFormation=-1;
		 int outOfBounds=0;
		 if(homeTeamInPossession)
            {
			homeSituation=getSituation(down,distance,ballOn,homeSituations,kickOffRequired,homeTeamInPossession,extraPointRequired,(homeTeamScore-awayTeamScore),null);	
			homePlay=getPlay(homeSituations,homePlayBook,homeSituation,homeSituations[homeSituation][14],random);
			homeFormation=getFormation(formations,homePlayBook[homePlay][2]);
			awaySituation=getSituation(down,distance,ballOn,awaySituations,kickOffRequired,!homeTeamInPossession,extraPointRequired,(awayTeamScore-homeTeamScore),formations[homeFormation]);	
			awayPlay=getPlay(awaySituations,awayPlayBook,awaySituation,awaySituations[awaySituation][14],random);
			awayFormation=getFormation(formations,awayPlayBook[awayPlay][2]);
            }
         else
            {   
			awaySituation=getSituation(down,distance,ballOn,awaySituations,kickOffRequired,!homeTeamInPossession,extraPointRequired,(awayTeamScore-homeTeamScore),null);	
			awayPlay=getPlay(awaySituations,awayPlayBook,awaySituation,awaySituations[awaySituation][14],random);
			awayFormation=getFormation(formations,awayPlayBook[awayPlay][2]);
			homeSituation=getSituation(down,distance,ballOn,homeSituations,kickOffRequired,homeTeamInPossession,extraPointRequired,(homeTeamScore-awayTeamScore),formations[awayFormation]);	
			homePlay=getPlay(homeSituations,homePlayBook,homeSituation,homeSituations[homeSituation][14],random);
			homeFormation=getFormation(formations,homePlayBook[homePlay][2]);
            }
         boolean defense=false;
         if(!kickOffRequired&&!homeTeamInPossession)
           {
           defense=true;	   
           }
		 int homeChosenPlayers[]=getPlayers(formations,homeDepthChart,homePlayers,homeFormation,homePlayBook[homePlay][1],defense);
         defense=false;
		 if(!kickOffRequired&&homeTeamInPossession)
		   {
		   defense=true;	   
		   }
		 int awayChosenPlayers[]=getPlayers(formations,awayDepthChart,awayPlayers,awayFormation,awayPlayBook[awayPlay][1],defense);
         int initialBallCarrier=0;
		 int secondaryBallCarrier=0;
		 int defenders[]=new int[3];
		 int recoverer=0;
		 int returner=0;
		 int offensiveYards=0;
		 int defensiveYards=0;
		 int kickYards=0;
		 int forcer=0;
		 int returnYards=0;
		 int description01=0;
		 int description02=0;
		 int injuredPlayer=0;
		 int injuryLength=0;
		 int[] animation=new int[45];
		 int fumbled=0;
		 int turnover=0;
		 int penaltyNumber=0;
		 boolean lossOfDown=false;
		 boolean autoFirstDown=false;
		 if(kickOffRequired)
		  {
		  int kickOffPlay=0;
		  int kickDefPlay=0;
		  int kickOffPlayers[][]=null;
		  int kickDefPlayers[][]=null;
	      int kickOffChosenPlayers[]=null;
		  int kickDefChosenPlayers[]=null;
		  if(homeTeamInPossession)
		    {
		    kickOffPlay=homePlayBook[homePlay][4];	
			kickDefPlay=awayPlayBook[awayPlay][4];
			kickOffPlayers=homePlayers;
			kickDefPlayers=awayPlayers;   	
      	    kickOffChosenPlayers=homeChosenPlayers;
			kickDefChosenPlayers=awayChosenPlayers;
		    }
		  else
		    {  
			kickOffPlay=awayPlayBook[awayPlay][4];
			kickDefPlay=homePlayBook[homePlay][4];
			kickOffPlayers=awayPlayers;
			kickDefPlayers=homePlayers;
			kickOffChosenPlayers=awayChosenPlayers;
			kickDefChosenPlayers=homeChosenPlayers;	
		    }
		  initialBallCarrier=kickOffPlayers[kickOffChosenPlayers[10]][0];  
		  if(kickOffPlay==24||kickOffPlay==25)
			{
			//Standard|Squib Kick
			int runNumber=shuffledActionCards[cardData[0]][1];
			int kickingSkills[]=getKickingSkills(kickOffPlayers[kickOffChosenPlayers[10]][1]);
			int kickOff=getSkill(false,kickOffChosenPlayers[10],kickOffPlayers,kickingSkills[0],runNumber,true);
			if(runNumber==1||runNumber==12)
			  {
			  playData=nextCard(playData,actionCards,random);
			  shuffledActionCards=playData.getShuffledActionCards();
			  cardData=playData.getCardData();	
			  runNumber=shuffledActionCards[cardData[0]][1];
			  kickOff=getSkill(false,kickOffChosenPlayers[10],kickOffPlayers,26,runNumber,true);
			  }	
			if(kickOffPlay==25)
			  {
			  kickOff+=15;	  
			  }
		    int modifier=0;	 
			if(kickOffPlay==24)
			  {	
			  playData=nextCard(playData,actionCards,random);
			  shuffledActionCards=playData.getShuffledActionCards();
			  cardData=playData.getCardData();	
			  runNumber=shuffledActionCards[cardData[0]][1];
			  modifier=getSkill(false,kickOffChosenPlayers[10],kickOffPlayers,67,runNumber,true);
			  animation[0]=modifier;
			  }
			else
			  {
			  modifier=1;	  
			  }
			playData=nextCard(playData,actionCards,random);
			shuffledActionCards=playData.getShuffledActionCards();
			cardData=playData.getCardData();	
			runNumber=(shuffledActionCards[cardData[0]][1])+modifier;
			outOfBounds=shuffledActionCards[cardData[0]][2];
			if(runNumber<1)
			  {
			  runNumber=1;	
			  }
			if(runNumber>12)
			  {
			  runNumber=12;	  
			  }
			if(random.nextInt(5)==0)
			  {
			  returner=13;	
			  }
			else
			  {
			  returner=11;	  
			  }
			boolean breakOut=false;
			if(runNumber==1)
			  {
			  playData=nextCard(playData,actionCards,random);
			  shuffledActionCards=playData.getShuffledActionCards();
			  cardData=playData.getCardData();		
			  int chanceOfBreakOut=getSkill(false,kickDefChosenPlayers[returner],kickDefPlayers,23,1,true);
			  playData=nextCard(playData,actionCards,random);
			  shuffledActionCards=playData.getShuffledActionCards();
			  cardData=playData.getCardData();
			  runNumber=shuffledActionCards[cardData[0]][1];
			  if(runNumber<=chanceOfBreakOut)
				{		
				breakOut=true;
				outOfBounds=shuffledActionCards[cardData[0]][2];	
				}
			  }
			if(runNumber==12)
			  {
			  fumble=true;	  
			  }
			int kickRet=getSkill(false,kickDefChosenPlayers[returner],kickDefPlayers,22,runNumber,true);
			int coverageMod=specialTeamsModifier(true,false,false,kickOffChosenPlayers,kickDefChosenPlayers,kickOffPlayers,kickDefPlayers);
			animation[1]=coverageMod;
			kickRet+=coverageMod;
            if(kickRet<5)
              {
              kickRet=5;	
              }
			if(breakOut)
			  {
			  kickRet=kickRet*2;	 
			  }
			if(kickDefPlay==33)
			  {
			  kickRet=kickRet/2;	  
			  }
			if(kickRet+kickOff>99)
			  {  
			  kickRet=100-kickOff;	
			  }
			maxYards=100-kickOff;  
            if(outOfBounds==0)
              {
              tacklers=specialTeamsTackler(true,false,false,false,breakOut,kickOffChosenPlayers,kickOffPlayers,random);
              defenders[0]=tacklers[0];
              defenders[1]=tacklers[1];
              }
            if(kickOffPlay==24&&kickDefPlay==33)
              {
              kickRet/=2;	    
              }
            returner=kickDefPlayers[kickDefChosenPlayers[returner]][0];  
            kickYards=ballOn-kickOff;
            returnYards=kickRet;
            changeOfPossession=true;
			} 
		  else
		    {
		    //Onside Kick	
		    int recovers=0;	
			int kickOff=ballOn-15;
		    if(kickDefPlay==33)
		      {		
		      recovers=8;	
		      }
		    else
		      {
		      recovers=12;	  
		      }
		    int passNumber=shuffledActionCards[cardData[0]][3];
			outOfBounds=shuffledActionCards[cardData[0]][2];
		    if(passNumber<=recovers)
		      { 
			  tacklers=specialTeamsTackler(true,true,true,false,false,kickDefChosenPlayers,kickDefPlayers,random);
			  if(outOfBounds==0)
				{
				defenders[0]=tacklers[0];
				defenders[1]=tacklers[1];
				}
			  if(tacklers[2]==0)
				{
				returner=kickOffPlayers[kickOffChosenPlayers[random.nextInt(4)]][0];	
				}
			  else
				{
				returner=kickOffPlayers[kickOffChosenPlayers[6+random.nextInt(4)]][0];
				}
		      }
		    else
		      { 
			  tacklers=specialTeamsTackler(true,true,false,false,false,kickOffChosenPlayers,kickOffPlayers,random);
			  if(outOfBounds==0)
				{
			    defenders[0]=tacklers[0];
			    defenders[1]=tacklers[1];
				}
			  if(tacklers[2]==0)
			    {
				returner=kickDefPlayers[kickDefChosenPlayers[onsideLeftRecoverers[random.nextInt(onsideLeftRecoverers.length-1)]]][0];	
				}
			  else
			    {
				returner=kickDefPlayers[kickDefChosenPlayers[onsideRightRecoverers[random.nextInt(onsideLeftRecoverers.length-1)]]][0];
			    }
			  homeTeamInPossession=!homeTeamInPossession;
			  }
			kickYards=10+random.nextInt(5);  
		    }
		  }
		 if(!kickOffRequired)
		  {
		  playData=playRunner(ballOn,distance,homeTeam,awayTeam,homePlayBook[homePlay],awayPlayBook[awayPlay],homeFormation,awayFormation,homeChosenPlayers,awayChosenPlayers,homePlayers,awayPlayers,homeCoverageTable,awayCoverageTable,homeTeamInPossession,extraPointRequired,playData,actionCards,database,random);
		  shuffledActionCards=playData.getShuffledActionCards();
          cardData=playData.getCardData();
          int playResult[]=playData.getPlayResults();
		  initialBallCarrier=playResult[0];
		  secondaryBallCarrier=playResult[1];
		  tacklers[0]=playResult[2];
		  tacklers[1]=playResult[3];
		  recoverer=playResult[4];
		  returner=playResult[5];
		  offensiveYards=playResult[6];
		  defensiveYards=playResult[7];  
		  returnYards=playResult[8];
		  animation[0]=playResult[9];
		  animation[1]=playResult[10];
		  animation[2]=playResult[11];
		  animation[3]=playResult[12];
		  animation[4]=playResult[13];
		  animation[5]=playResult[14];
		  animation[6]=playResult[15];
		  animation[7]=playResult[16];
		  animation[8]=playResult[17];
		  animation[9]=playResult[18];
		  animation[10]=playResult[19];
		  animation[11]=playResult[20];
		  animation[12]=playResult[21];
		  animation[13]=playResult[22];
		  animation[14]=playResult[23];
		  animation[15]=playResult[24];
		  animation[16]=playResult[25];
		  animation[17]=playResult[26];
		  animation[18]=playResult[27];
		  animation[19]=playResult[28];
		  animation[20]=playResult[29];
		  fumbled=playResult[30];
		  turnover=playResult[31];
		  if(turnover==1)
		    {
		    changeOfPossession=true;	
		    }
		  tacklers[2]=playResult[32];
		  tacklers[3]=playResult[33];
		  animation[21]=playResult[34];
		  animation[22]=playResult[35];
		  animation[23]=playResult[36];
		  animation[24]=playResult[37];
		  kickYards=playResult[38];
		  forcer=playResult[39];
		  outOfBounds=playResult[40];
		  penaltyNumber=playResult[41];
		  animation[25]=playResult[42];
		  animation[26]=playResult[43];
		  animation[27]=playResult[44];
		  animation[28]=playResult[45];
		  animation[29]=playResult[46];	
		  animation[30]=playResult[47];
		  animation[31]=playResult[48];
		  animation[32]=playResult[49];
		  animation[33]=playResult[50];
		  animation[34]=playResult[51];	
		  animation[35]=playResult[52];
		  animation[36]=playResult[53];
		  animation[37]=playResult[54];
		  animation[38]=playResult[55];
		  animation[39]=playResult[56];	
		  animation[40]=playResult[57];
		  animation[41]=playResult[58];
		  animation[42]=playResult[59];
		  animation[43]=playResult[60];
		  animation[44]=playResult[61];	
		  description01=playResult[62];
		  description02=playResult[63];		
		  }
//		 if(cardData[1]>0)
//		   {
//		   int[] zData=zCardProcess(cardData,actionCards,shuffledActionCards,random,offensiveYards);  
//		   cardData[0]=zData[0];
//		   cardData[1]=zData[1];
//		   cardData[2]=zData[2];
//		   cardData[3]=zData[3];
//		   offensiveYards=zData[4];
//		   if(offensiveYards>maxYards)
//		     {
//		     offensiveYards=maxYards;	
//		     }
//		   }
		//Store play results to database  
		int homeTeamInPossessionInt=0;
		int offensiveFormation=0;
		int defensiveFormation=0;
		int offensivePlay=0;
		int defensivePlay=0;
		int primaryStrategyNumber=0;
		int secondaryStrategies[]=new int[10];
		int offensivePlayers[]=new int[11];
		int defensivePlayers[]=new int[11];
		int offensivePenaltyYards=0;
		int defensivePenaltyYards=0;
		if(homeTeamInPossession)
		  {
		  homeTeamInPossessionInt=1;
		  offensiveFormation=homeFormation;
		  defensiveFormation=awayFormation;
		  offensivePlay=homePlayBook[homePlay][4];
		  defensivePlay=awayPlayBook[awayPlay][4];	
		  primaryStrategyNumber=homePlayBook[homePlay][5];
		  for(int currentStrategy=0;currentStrategy<10;currentStrategy++)
		     {
			 secondaryStrategies[currentStrategy]=homePlayBook[homePlay][5+currentStrategy];	  
		     }
		  int offensivePlayer=0;
		  int defensivePlayer=0;   
		  for(int currentPlayer=0;currentPlayer<homeChosenPlayers.length;currentPlayer++)
		     {   
		     if(homeChosenPlayers[currentPlayer]!=-1)
		       {
		       offensivePlayers[offensivePlayer]=homePlayers[homeChosenPlayers[currentPlayer]][0];
		       offensivePlayer++;		
		       }
			 if(awayChosenPlayers[currentPlayer]!=-1&&formations[defensiveFormation][0]==16)
			   {
			   defensivePlayers[defensivePlayer]=awayPlayers[awayChosenPlayers[currentPlayer]][0];
			   defensivePlayer++;		
			   }		       
		     }
		  }
		else
		  {
		  offensiveFormation=awayFormation;
		  defensiveFormation=homeFormation;
		  offensivePlay=awayPlayBook[awayPlay][4];
		  defensivePlay=homePlayBook[homePlay][4];	
		  primaryStrategyNumber=awayPlayBook[awayPlay][5];  
		  for(int currentStrategy=0;currentStrategy<10;currentStrategy++)
			 {
			 secondaryStrategies[currentStrategy]=awayPlayBook[awayPlay][5+currentStrategy];	  
			 }
		  int offensivePlayer=0;
		  int defensivePlayer=0;   
		  for(int currentPlayer=0;currentPlayer<homeChosenPlayers.length;currentPlayer++)
		     {   
			 if(awayChosenPlayers[currentPlayer]!=-1)
			   {
			   offensivePlayers[offensivePlayer]=awayPlayers[awayChosenPlayers[currentPlayer]][0];
			   offensivePlayer++;		
			   }
			 if(homeChosenPlayers[currentPlayer]!=-1)
			   {
			   defensivePlayers[defensivePlayer]=homePlayers[homeChosenPlayers[currentPlayer]][0];
			   defensivePlayer++;		
			   }		       
			 }
		  }
		try
		  {
		  Statement sql=database.createStatement();	
		  int updates=sql.executeUpdate("INSERT INTO playbyplay (" +
									    "Fixture,Clock,HomeTeamInPossession,Down,Distance,DistanceToGoal," +
									    "OffensiveFormationNumber,DefensiveFormationNumber," +
									    "OffensivePlayNumber,DefensivePlayNumber,PrimaryStrategyNumber," +
									    "SecondaryStrategyNumber1,SecondaryStrategyNumber2," +
			                            "SecondaryStrategyNumber3,SecondaryStrategyNumber4," +
			                            "SecondaryStrategyNumber5,SecondaryStrategyNumber6," +
			                            "SecondaryStrategyNumber7,SecondaryStrategyNumber8," +
			                            "SecondaryStrategyNumber9,SecondaryStrategyNumber10," +
			                            "InitialBallCarrier,SecondaryBallCarrier," +
			                            "Defender1,Defender2,Defender3,Defender4,Forcer,Recoverer," +
			                            "KickReturner,OffensiveYards,DefensiveYards,KickYards,ReturnYards," +
			                            "Description01,Description02," +
			                            "OffensivePlayer01,OffensivePlayer02,OffensivePlayer03," +
			                            "OffensivePlayer04,OffensivePlayer05,OffensivePlayer06," +
			                            "OffensivePlayer07,OffensivePlayer08,OffensivePlayer09," +
			                            "OffensivePlayer10,OffensivePlayer11," +
			                            "DefensivePlayer01,DefensivePlayer02,DefensivePlayer03," +
			                            "DefensivePlayer04,DefensivePlayer05,DefensivePlayer06," +
			                            "DefensivePlayer07,DefensivePlayer08,DefensivePlayer09," +
			                            "DefensivePlayer10,DefensivePlayer11," +
			                            "InjuredPlayer,InjuredPlayerLength," +
			                            "Animation01,Animation02,Animation03,Animation04,Animation05," +
			                            "Animation06,Animation07,Animation08,Animation09,Animation10," +
			                            "Animation11,Animation12,Animation13,Animation14,Animation15," +
			                            "Animation16,Animation17,Animation18,Animation19,Animation20," +
			                            "Animation21,Animation22,Animation23,Animation24,Animation25," +
										"Animation26,Animation27,Animation28,Animation29,Animation30," +
			                            "Animation31,Animation32,Animation33,Animation34,Animation35," +
			                            "Animation36,Animation37,Animation38,Animation39,Animation40," +
			                            "Animation41,Animation42,Animation43,Animation44,Animation45," +
			                            "Fumbled,Turnover,Penalty,OutOfBounds,League," +			                            									    "DateTimeStamp) " +
									    "VALUES (" +
									    fixtureNumber + "," +
									    timeGone + "," +
									    homeTeamInPossessionInt + "," +
                                        down + "," +
                                        distance + "," +
                                        ballOn + "," +
                                        formations[offensiveFormation][0] + "," +
                                        formations[defensiveFormation][0] + "," +
                                        offensivePlay + "," +
                                        defensivePlay + "," +
                                        primaryStrategyNumber + "," +
                                        secondaryStrategies[0] + "," +
			                            secondaryStrategies[1] + "," +
			                            secondaryStrategies[2] + "," +
			                            secondaryStrategies[3] + "," +
			                            secondaryStrategies[4] + "," +
			                            secondaryStrategies[5] + "," +
			                            secondaryStrategies[6] + "," +
			                            secondaryStrategies[7] + "," +
			                            secondaryStrategies[8] + "," +
			                            secondaryStrategies[9] + "," +
			                            initialBallCarrier + "," +
			                            secondaryBallCarrier + "," +
                                        tacklers[0] + "," +
                                        tacklers[1] + "," +
			                            tacklers[2] + "," +
										tacklers[3] + "," +
										forcer + "," +
			                            recoverer + "," +
                                        returner + "," +
                                        offensiveYards + "," +
                                        defensiveYards + "," +
                                        kickYards + "," +  
                                        returnYards + "," +
                                        description01 + "," +
                                        description02 + "," +
                                        offensivePlayers[0] + "," +
		                                offensivePlayers[1] + "," +
		                                offensivePlayers[2] + "," +
		                                offensivePlayers[3] + "," +
		                                offensivePlayers[4] + "," +
		                                offensivePlayers[5] + "," +
		                                offensivePlayers[6] + "," +
		                                offensivePlayers[7] + "," +
		                                offensivePlayers[8] + "," +
		                                offensivePlayers[9] + "," +
		                                offensivePlayers[10] + "," +
		                                defensivePlayers[0] + "," +
		                                defensivePlayers[1] + "," +
		                                defensivePlayers[2] + "," +
		                                defensivePlayers[3] + "," +
		                                defensivePlayers[4] + "," +
		                                defensivePlayers[5] + "," +
		                                defensivePlayers[6] + "," +
		                                defensivePlayers[7] + "," +
		                                defensivePlayers[8] + "," +
		                                defensivePlayers[9] + "," +
		                                defensivePlayers[10] + "," +
                                        injuredPlayer + "," +
                                        injuryLength + "," +
                                        animation[0] + "," +
			                            animation[1] + "," +
			                            animation[2] + "," +
			                            animation[3] + "," +
			                            animation[4] + "," +
			                            animation[5] + "," +
			                            animation[6] + "," +
			                            animation[7] + "," +
			                            animation[8] + "," +
			                            animation[9] + "," +
			                            animation[10] + "," +
			                            animation[11] + "," +
			                            animation[12] + "," +
			                            animation[13] + "," +
			                            animation[14] + "," +
			                            animation[15] + "," +
			                            animation[16] + "," +
			                            animation[17] + "," +
			                            animation[18] + "," +
			                            animation[19] + "," +
			                            animation[20] + "," +
										animation[21] + "," +
										animation[22] + "," +
										animation[23] + "," +
										animation[24] + "," +
			                            animation[25] + "," +
										animation[26] + "," +
										animation[27] + "," +
										animation[28] + "," +
										animation[29] + "," +
			                            animation[30] + "," +
										animation[31] + "," +
										animation[32] + "," +
										animation[33] + "," +
										animation[34] + "," +
			                            animation[35] + "," +
										animation[36] + "," +
										animation[37] + "," +
										animation[38] + "," +
										animation[39] + "," +
			                            animation[40] + "," +
										animation[41] + "," +
										animation[42] + "," +
										animation[43] + "," +
										animation[44] + "," +
                                        fumbled + "," +
                                        turnover + "," +
                                        penaltyNumber + "," +
                                        outOfBounds + "," +
                                        leagueNumber + ",'" +
									    Routines.getDateTime(false) + "')");
			if(updates!=1)
			  {
			  Routines.writeToLog(servletName,"New playbyplay not created, reason unknown",false,context);	
			  }
	      }  
		catch(SQLException error)
		   {
		   Routines.writeToLog(servletName,"Error writing to playbyplay : " + error,false,context);
		   }
		 if(kickOffRequired) 
		   {
		   kickOffRequired=false;
		   }
         int oldBallOn=ballOn;
		 ballOn=(ballOn-offensiveYards-kickYards-defensivePenaltyYards)+defensiveYards+returnYards+offensivePenaltyYards;
		 if(offensivePlay==27||offensivePlay==29||offensivePlay==30)
		   {
		   if((turnover==1||kickYards<0)&&distance-kickYards-offensiveYards-defensivePenaltyYards<1)
			  {
			  down=0;	
			  if(ballOn<10)
				{
				distance=ballOn;
				}
			  else
				{
				distance=10;	  	 
				}
			  }
		   }
		 else
		   {  
		   if(distance-offensiveYards-defensivePenaltyYards<1)
		     {
		     down=0;	
		     if(ballOn<10)
			   {
			   distance=ballOn;
			   }
		     else
			   {
			   distance=10;	  	 
			   }
		     }
		   else
		     {
		     distance=distance-offensiveYards-defensivePenaltyYards+offensivePenaltyYards;	  
		     }
		   }  
         if(offensivePlay==29||offensivePlay==30)
           {
           if(kickYards>0&&turnover==1)	
             {
             changeOfPossession=false;	
             }
           if(kickYards>0&&turnover==0)
             {
             changeOfPossession=true;
             if(ballOn==0)
               {
               ballOn=20;	
               }
             }  
           }
		 if(offensivePlay==27)
		   {
		   if(kickYards>0)
			 {	
			 kickYards-=17;
			 ballOn+=17;
			 }	
		   if(ballOn==0)
			 {
			 if(homeTeamInPossession)
			   {	
			   if(extraPointRequired)
				 {
				 homeTeamScore+=1;	
				 }
			   else
				 { 
				 homeTeamScore+=3; 
				 }
			   }  
			 else
			   {                 			   
			   if(extraPointRequired)
				 {
				 awayTeamScore+=1;		
				 }
			   else
				 { 
				 awayTeamScore+=3; 
				 }
			   }
      		 down=-1;
			 kickOffRequired=true;
			 ballOn=70;
			 distance=0;
			 }
		   else
			 {
			 changeOfPossession=true;		   
			 }
		   extraPointRequired=false;
		   }
		 if(extraPointRequired&&offensivePlay!=27)
		   {
		   if(offensiveYards>2)
			 {
			 if(homeTeamInPossession)
			   {	
			   homeTeamScore+=2;
			   }
			 else
			   {
			   awayTeamScore+=2;	  
			   }
			 }
		   extraPointRequired=false;
		   down=-1;
		   kickOffRequired=true;
		   ballOn=70;
		   distance=0;	
		   }
		 if(ballOn<0)
		   {
		   Routines.writeToLog(servletName,"["+fixtureNumber+"]Play past goalline",false,context);
		   }
		 if((ballOn==0&&offensivePlay!=27&&offensivePlay!=29&&offensivePlay!=30)||
		    (ballOn==100&&changeOfPossession)||(ballOn==100&&returnYards>0)||(ballOn==0&&(offensivePlay==27||offensivePlay==29||offensivePlay==30)&&(turnover==1||kickYards<0)))
		   { 
		   down=-1;
		   extraPointRequired=true;
		   ballOn=3;
		   distance=3;
		   if(changeOfPossession)
		     {	 
		     if(homeTeamInPossession)
		       {
		       awayTeamScore+=6;	
		       }
		     else
		       {
		       homeTeamScore+=6;	  
		       }
		     }
		   else
		     if(homeTeamInPossession)
			   {
			   homeTeamScore+=6;	
			   }
		     else
			   {
			   awayTeamScore+=6;	  
			   }		       
		   }
		 if(ballOn>100)
		   { 
		   Routines.writeToLog(servletName,"["+fixtureNumber+"]Play too far into own endzone",false,context);	 
		   }
	     if(ballOn>99&&returnYards<=0)
		   { 
		   down=-1;	
		   kickOffRequired=true;
		   ballOn=80;
		   distance=0; 
		   if(homeTeamInPossession)
			 {
			 awayTeamScore+=2;	
			 }
		   else
			 {
			 homeTeamScore+=2;	  
			 }
		   }
		 if(ballOn!=0&&penaltyNumber==0)
		   {  		   		   
		   down++;
		   }
		 if(down>4||changeOfPossession)
		   {
		   homeTeamInPossession=!homeTeamInPossession;
		   if(!extraPointRequired)
		     {
		     down=1;
		     ballOn=100-ballOn;	
		     if(ballOn<10)
			   {
			   distance=ballOn;
			   }
		     else
			   {
			   distance=10;	  	 
			   }
		     }  
		   changeOfPossession=false;	 
		   }
         if((timeGone>=3600&&homeTeamScore!=awayTeamScore)||
		    (timeGone>=4500&&!postSeason))
		    {
			endOfGame=true;		
		    }
		 //test code
		 testCounter++;
		 //end of test code   
         }
	  Routines.writeToLog(servletName,"["+fixtureNumber+"]Final Score : "+awayTeamScore+"-"+homeTeamScore,false,context);  
      pool.returnConnection(database);
      database=null;   
	  active=false;
	  }

    private int getSituation(int down,
                             int distance,
                             int ballOn,
                             int[][] situations,
                             boolean kickOffRequired,
                             boolean inPossession,
                             boolean extraPointRequired,
                             int pointsDifference,
                             int[] offensiveFormation)
       {
       boolean shotGun=false;	
       boolean punt=false;
       boolean fieldGoal=false;
       int numOfWR=0;
       int numOfRB=0;
       if(!inPossession&&!kickOffRequired)
         {
		 if(offensiveFormation[0]==35)
		   {
		   punt=true;
		   }         	
         if(offensiveFormation[0]==33)
           {
		   fieldGoal=true;
		   }
		 for(int currentPosition=0;currentPosition<offensiveFormation.length;currentPosition++)
           {
           if(offensiveFormation[currentPosition]==13)
             {
             numOfWR++;				   
             }
		   if(offensiveFormation[currentPosition]==15||offensiveFormation[currentPosition]==16)
			 {
			 numOfRB++;				   
			 }  
		   }
         }
       int situationArrayEntry=-1;
       if(pointsDifference>22)
         {
         pointsDifference=22;	
         }
       if(pointsDifference<-22)
         {
         pointsDifference=-22;	
         }	  	
	   //Offense
	   if(!kickOffRequired&&inPossession&&!extraPointRequired)
		 {
		 for(int currentSituation=0;currentSituation<situations.length;currentSituation++)
			{
			if((situations[currentSituation][1]==0||(situations[currentSituation][1]==2&&down==4))&&
			  ((situations[currentSituation][2]==1&&down==1)||
			   (situations[currentSituation][3]==1&&down==2)||
			   (situations[currentSituation][4]==1&&down==3)||
			   (situations[currentSituation][5]==1&&down==4))&&
				situations[currentSituation][6]<=(timeGone/60)&&
				situations[currentSituation][7]>=(timeGone/60)&&
				situations[currentSituation][8]<=distance&&
				situations[currentSituation][9]>=distance&&
				situations[currentSituation][10]<=ballOn&&
				situations[currentSituation][11]>=ballOn&&				
				situations[currentSituation][12]<=pointsDifference&&
				situations[currentSituation][13]>=pointsDifference)
			   {
			   situationArrayEntry=currentSituation;				
			   currentSituation=situations.length;
			   }
			}
		if(situationArrayEntry==-1)
		  {  
		  Routines.writeToLog(servletName,"["+fixtureNumber+"]No offensive situation found for fixture",false,context);
		  }					 	
		} 
	  //Defense
	  if(!kickOffRequired&&!inPossession&&!extraPointRequired)
		{
	    for(int currentSituation=0;currentSituation<situations.length;currentSituation++)
		   {
		   if((situations[currentSituation][1]==1||(situations[currentSituation][1]==2&&situations[currentSituation][15]==17))&&
			 ((situations[currentSituation][2]==1&&down==1)||(situations[currentSituation][3]==1&&down==2)||(situations[currentSituation][4]==1&&down==3)||(situations[currentSituation][5]==1&&down==4))&&
			   situations[currentSituation][6]<=(timeGone/60)&&
			   situations[currentSituation][7]>=(timeGone/60)&&
			   situations[currentSituation][8]<=distance&&
			   situations[currentSituation][9]>=distance&&
			   situations[currentSituation][10]<=ballOn&&
			   situations[currentSituation][11]>=ballOn&&				
			   situations[currentSituation][12]<=pointsDifference&&
			   situations[currentSituation][13]>=pointsDifference)
			   {
			   boolean shotGunCorrect=false;
			   boolean wrCorrect=false;
			   boolean rbCorrect=false;	
			   for(int currentCall=15;currentCall<36;currentCall++)
			      {	
			      if((situations[currentSituation][currentCall]==4&&shotGun))
			        {
			        shotGunCorrect=true;	
			        }	
				  if((situations[currentSituation][currentCall]==9&&numOfWR==0)||
					(situations[currentSituation][currentCall]==10&&numOfWR==1)||
					(situations[currentSituation][currentCall]==11&&numOfWR==2)||
					(situations[currentSituation][currentCall]==12&&numOfWR==3)||
					(situations[currentSituation][currentCall]==13&&numOfWR==4)||
					(situations[currentSituation][currentCall]==14&numOfWR==5))
					{
					wrCorrect=true;	
					}	
				  if((situations[currentSituation][currentCall]==5&&numOfRB==0)||
					(situations[currentSituation][currentCall]==6&&numOfRB==1)||
					(situations[currentSituation][currentCall]==7&&numOfRB==2)||
					(situations[currentSituation][currentCall]==8&&numOfRB==3))
					{
					rbCorrect=true;	
					}						  		          
			      }
			   if(((shotGunCorrect&&shotGun)||(!shotGunCorrect&&!shotGun))&&((wrCorrect&&rbCorrect)||(punt||fieldGoal)))
			     {   
			     situationArrayEntry=currentSituation;				
			     currentSituation=situations.length;
			     }
			}
		  }  
		if(situationArrayEntry==-1)
		  {  
		  Routines.writeToLog(servletName,"["+fixtureNumber+"]No defensive situation found for fixture",false,context);
		  }	
		}		  
       //KickOff
       if(kickOffRequired&&inPossession)
         {
		 //Kicking 
         for(int currentSituation=0;currentSituation<situations.length;currentSituation++)
            {
            if(situations[currentSituation][1]==2&&
               situations[currentSituation][15]==16&&
			   situations[currentSituation][6]<=(timeGone/60)&&
			   situations[currentSituation][7]>=(timeGone/60)&&
			   situations[currentSituation][12]<=pointsDifference&&
			   situations[currentSituation][13]>=pointsDifference)
              {
              situationArrayEntry=currentSituation;				
              currentSituation=situations.length;
              }
            }
		 if(situationArrayEntry==-1)
		   {  
		   Routines.writeToLog(servletName,"["+fixtureNumber+"]No kickoff situation found for fixture",false,context);
		   }	
          }  
	   if(kickOffRequired&&!inPossession)
		 {            
		 //Kick Return 
		 for(int currentSituation=0;currentSituation<situations.length;currentSituation++)
		    {
			if(situations[currentSituation][1]==2&&
			   situations[currentSituation][15]==18&&
			   situations[currentSituation][6]<=(timeGone/60)&&
			   situations[currentSituation][7]>=(timeGone/60)&&
			   situations[currentSituation][12]<=pointsDifference&&
			   situations[currentSituation][13]>=pointsDifference)
			   {
			   situationArrayEntry=currentSituation;
			   currentSituation=situations.length;				
			   }
			}            
		 if(situationArrayEntry==-1)
		   {  
		   Routines.writeToLog(servletName,"["+fixtureNumber+"]No kickret situation found for fixture",false,context);
		   }	
         }
		//ExtraPoint
		if(extraPointRequired&&inPossession)
		  {
		  boolean attemptFound=false;	
		  //Looking for 2PT attempt 
		  for(int currentSituation=0;currentSituation<situations.length;currentSituation++)
			 {
			 if(situations[currentSituation][1]==0&&
				situations[currentSituation][6]<=(timeGone/60)&&
				situations[currentSituation][7]>=(timeGone/60)&&
				situations[currentSituation][12]<=pointsDifference&&
				situations[currentSituation][13]>=pointsDifference)
			   {
			   for(int currentCall=15;currentCall<36;currentCall++)
			      {	
			      if((situations[currentSituation][currentCall]==19))
				    {
				    attemptFound=true;	
				    situationArrayEntry=currentSituation;				
				    currentSituation=situations.length;
				    currentCall=36;
					}	
			      }
			   }
			 }
		  if(!attemptFound)
			{ 
			//Look for 1PT attempt	
			for(int currentSituation=0;currentSituation<situations.length;currentSituation++)
			   {
			   if(situations[currentSituation][1]==2&&
				  situations[currentSituation][6]<=(timeGone/60)&&
				  situations[currentSituation][7]>=(timeGone/60)&&
				  situations[currentSituation][12]<=pointsDifference&&
				  situations[currentSituation][13]>=pointsDifference)
				 {
			     for(int currentCall=15;currentCall<36;currentCall++)
				    {	
				    if((situations[currentSituation][currentCall]==21))
				      {
				      attemptFound=true;	
				      situationArrayEntry=currentSituation;				
				      currentSituation=situations.length;
					  currentCall=36;
					  }	
				    }
				 }
			   }
			}
			if(!attemptFound)
			  { 
			  //Look for any FG attempt	situation to use.
			  for(int currentSituation=0;currentSituation<situations.length;currentSituation++)
				 {
				 if(situations[currentSituation][1]==2&&
					situations[currentSituation][6]<=(timeGone/60)&&
					situations[currentSituation][7]>=(timeGone/60)&&
					situations[currentSituation][12]<=pointsDifference&&
					situations[currentSituation][13]>=pointsDifference)
				   {
				   for(int currentCall=15;currentCall<36;currentCall++)
					  {	
					  if((situations[currentSituation][currentCall]==15))
						{
						attemptFound=true;	
						situationArrayEntry=currentSituation;				
						currentSituation=situations.length;
						currentCall=36;
						System.out.println("Defaulted to 1PT");
						}	
					  }
				   }
				 }
			  }	
			if(!attemptFound)
			  { 
			  //Look for any situation to use.
			  for(int currentSituation=0;currentSituation<situations.length;currentSituation++)
				 {
				 if(situations[currentSituation][1]==0&&
				    situations[currentSituation][2]==1&&
					situations[currentSituation][8]<=10&&
					situations[currentSituation][9]>=10&&
					situations[currentSituation][10]<=50&&
					situations[currentSituation][11]>=50)
				   {
				   attemptFound=true;	
				   situationArrayEntry=currentSituation;				
				   currentSituation=situations.length;
				   System.out.println("Defaulted 2PT");
			       }  
				 }
			  }				  		
		  if(situationArrayEntry==-1)
			{  
			Routines.writeToLog(servletName,"["+fixtureNumber+"]No XP situation found for fixture",false,context);
			}	
		   }  
		if(extraPointRequired&&!inPossession)
		  {            
		  //KickDef
		  boolean attemptFound=false;	
		  //Looking for 2PT attempt 
		  for(int currentSituation=0;currentSituation<situations.length;currentSituation++)
			 {
			 if(situations[currentSituation][1]==1&&
				situations[currentSituation][6]<=(timeGone/60)&&
				situations[currentSituation][7]>=(timeGone/60)&&
				situations[currentSituation][12]<=pointsDifference&&
				situations[currentSituation][13]>=pointsDifference)
			   {
			   for(int currentCall=15;currentCall<36;currentCall++)
				  {	
				  if((situations[currentSituation][currentCall]==20))
					{
					attemptFound=true;	
					situationArrayEntry=currentSituation;				
					currentSituation=situations.length;
					currentCall=36;
					}	
				  }
			   }
			 }
		  if(!attemptFound)
			{ 
			//Look for 1PT attempt	
			for(int currentSituation=0;currentSituation<situations.length;currentSituation++)
			   {
			   if(situations[currentSituation][1]==2&&
				  situations[currentSituation][6]<=(timeGone/60)&&
				  situations[currentSituation][7]>=(timeGone/60)&&
				  situations[currentSituation][12]<=pointsDifference&&
				  situations[currentSituation][13]>=pointsDifference)
				 {
				 for(int currentCall=15;currentCall<36;currentCall++)
					{	
					if((situations[currentSituation][currentCall]==22))
					  {
					  attemptFound=true;	
					  situationArrayEntry=currentSituation;				
					  currentSituation=situations.length;
					  currentCall=36;
					  }	
					}
				 }
			   }
			}
			if(!attemptFound)
			  { 
			  //Look for any FG attempt	situation to use.
			  for(int currentSituation=0;currentSituation<situations.length;currentSituation++)
				 {
				 if(situations[currentSituation][1]==2&&
					situations[currentSituation][6]<=(timeGone/60)&&
					situations[currentSituation][7]>=(timeGone/60)&&
					situations[currentSituation][12]<=pointsDifference&&
					situations[currentSituation][13]>=pointsDifference)
				   {
				   for(int currentCall=15;currentCall<36;currentCall++)
					  {	
					  if((situations[currentSituation][currentCall]==17))
						{
						attemptFound=true;	
						situationArrayEntry=currentSituation;				
						currentSituation=situations.length;
						currentCall=36;
						}	
					  }
				   }
				 }
			  }	
			if(!attemptFound)
			  { 
//			  //Look for any situation to use.
			  for(int currentSituation=0;currentSituation<situations.length;currentSituation++)
				 {
				 if(situations[currentSituation][1]==1&&
					situations[currentSituation][2]==1&&
					situations[currentSituation][8]<=10&&
					situations[currentSituation][9]>=10&&
					situations[currentSituation][10]<=50&&
					situations[currentSituation][11]>=50)
				   {
				   attemptFound=true;	
				   situationArrayEntry=currentSituation;				
				   currentSituation=situations.length;
				   }  
				 }
			  }          
		  if(situationArrayEntry==-1)
			{  
			Routines.writeToLog(servletName,"["+fixtureNumber+"]No XPdef situation found for fixture",false,context);
			}	
		  }         
       return situationArrayEntry;	
       }
       
	private int getPlay(int[][] situations,
	                    int[][] playBook,
	                    int situation,
						int playOrderNumber,
						Random random)
	   {
	   int playArrayNum=0;
	   int numOfPlays=0;
	   int situationNumber=situations[situation][0];
	   for(int currentEntry=0;currentEntry<playBook.length;currentEntry++)
	      {
	      if(playBook[currentEntry][0]==situationNumber)
	        {
	        numOfPlays++;		
	        }
	      }
	   int[] selectedPlays=new int[numOfPlays];
	   numOfPlays=0;
	   int runOrPass=random.nextInt(2);
	   for(int currentEntry=0;currentEntry<playBook.length;currentEntry++)
		  {
		  if((playOrderNumber!=4&&playBook[currentEntry][0]==situationNumber)||
		     (playOrderNumber==4&&playBook[currentEntry][16]==runOrPass))
			{
			selectedPlays[numOfPlays]=currentEntry;
			numOfPlays++;
            }		
		  }
	   //Random	  
	   if(playOrderNumber==1||playOrderNumber==4)
	     {
		 int playNumber=(random.nextInt(numOfPlays));	
		 playArrayNum=selectedPlays[playNumber];
		 situations[situation][sizeOfSituationsArray-1]=selectedPlays[playNumber];
		 }
	   //Next	  
	   if(playOrderNumber==2)
		 {	 
		 situations[situation][sizeOfPlaysArray-1]++;
		 if(situations[situation][sizeOfPlaysArray-1]>=numOfPlays||
		    situations[situation][sizeOfPlaysArray-1]==-998)
		   {
		   situations[situation][sizeOfPlaysArray-1]=0;		 	    
		   }
		 playArrayNum=selectedPlays[situations[situation][sizeOfPlaysArray-1]];  
		 }
	   //OnStop	  
	   if(playOrderNumber==3)
		 {
		 if(situations[situation][sizeOfPlaysArray-1]==-999)
		   {
		   situations[situation][sizeOfPlaysArray-1]=0;			  	    
		   }
		 else
		   {
		   if(selectedPlays[situations[situation][sizeOfPlaysArray-1]]<=0)
		     {
			 situations[situation][sizeOfPlaysArray-1]++;
			 if(situations[situation][sizeOfPlaysArray-1]>=numOfPlays)
			   {
			   situations[situation][sizeOfPlaysArray-1]=0;		 	    
			   }						  
		     }
		   }
		 playArrayNum=selectedPlays[situations[situation][sizeOfPlaysArray-1]];  
		 }
	   //Weighted	  
	   if(playOrderNumber>=5&&playOrderNumber<=13)
		 {
		 int numOfWeights=0; 
		 for(int currentElement=0;currentElement<selectedPlays.length;currentElement++)
		    {
		    if(playBook[selectedPlays[currentElement]][sizeOfPlaysArray-1]==0)
		      {			  	    
			  playBook[selectedPlays[currentElement]][sizeOfPlaysArray-1]=1;	
		      }
		    numOfWeights+=playBook[selectedPlays[currentElement]][sizeOfPlaysArray-1];  
		    }
		 numOfWeights=(random.nextInt(numOfWeights)+1);   
		 for(int currentElement=0;currentElement<selectedPlays.length;currentElement++)
			{
			numOfWeights-=playBook[selectedPlays[currentElement]][sizeOfPlaysArray-1]; 	
			if(numOfWeights<=0)
			  {			  	    
			  playArrayNum=selectedPlays[currentElement];
			  situations[situation][sizeOfSituationsArray-1]=selectedPlays[currentElement];
			  currentElement=selectedPlays.length;	
			  }
			}		    
		 }
	   return playArrayNum;	
	   }       
	
	private int getFormation(int[][] formations,
	                         int formationNumber)
	   {
	   int formationArrayNum=0;	
	   for(int currentFormation=0;currentFormation<formations.length;currentFormation++)
		  {
		  if(formations[currentFormation][0]==formationNumber)
			{
			formationArrayNum=currentFormation;	
			currentFormation=formations.length;			
			}
		  }	   
	   return formationArrayNum;
	   }	
	   
	private int[] getPlayers(int[][] formations,
	                         int[][] depthCharts,
	                         int[][] players,
	                         int formationArrayNumber,
	                         int setNumber,
	                         boolean defense)
	   {
	   int[] formation=new int[16];	
	   int selectedPlayers[]=new int[21];
	   for(int currentInt=0;currentInt<16;currentInt++)
	      {
	      formation[currentInt]=formations[formationArrayNumber][currentInt];	
	      }
	   if(defense)
	     {
	     //Switch RCB and Nickel/Dime back position to force routine to give RCB the best CB.
	     int tempPosition=formation[12];
		 formation[12]=formation[15];
		 formation[15]=tempPosition; 	
	     }
	   for(int currentPlayer=0;currentPlayer<selectedPlayers.length;currentPlayer++)
		  {
		  selectedPlayers[currentPlayer]=-1;
		  }
	   boolean allPositionsFound=false;
	   int currentLoop=-1;
	   while(!allPositionsFound)
	   	  {
	   	  currentLoop++;	
	   	  int selectedPlayerPosition=0;	
          for(int currentFormationPosition=1;currentFormationPosition<16;currentFormationPosition++)
	         {	
	         selectedPlayerPosition++;	
	         //Two players lined up in the same position cause the positionNumber to be negative, so multiply by
	         //-1 in order to get the real positionNumber.
		     boolean doubledUp=false;
		     boolean hasBeenDoubledUp=false;
		     boolean playerFound=false;
		     boolean positionFilled=false;
		     int positionNumber=0;
		     positionNumber=formation[currentFormationPosition];
		     if(positionNumber<0)
			   {
			   doubledUp=true;
			   hasBeenDoubledUp=true;
			   positionNumber*=-1;		
			   }	
		     //Loop through the depthChart data to find the best unused player for that position. 	
		     if(positionNumber>0)
		       {
		       if(currentLoop>0&&(selectedPlayers[selectedPlayerPosition-1]!=-1||(doubledUp&&selectedPlayers[selectedPlayerPosition]!=-1)))
		         {	
		         positionFilled=true;	
		         }
			   boolean chartEmpty=true;		
		       for(int currentDepthChart=0;currentDepthChart<depthCharts.length;currentDepthChart++)
	              {
	              if((selectedPlayers[selectedPlayerPosition-1]==-1||(doubledUp&&selectedPlayers[selectedPlayerPosition]==-1))
	              &&((depthCharts[currentDepthChart][0]==setNumber&&depthCharts[currentDepthChart][1]==positionDepthCharts[positionNumber][currentLoop])
	              ||(positionNumber==35&&depthCharts[currentDepthChart][1]==35)
	              ||(positionNumber==3&&depthCharts[currentDepthChart][1]==3)
	              ||(positionNumber==4&&depthCharts[currentDepthChart][1]==4)))
	                 {	
	                 chartEmpty=false;	
	                 boolean alreadySelected=false;	
	                 for(int currentPlayer=0;currentPlayer<17;currentPlayer++)
	                    {
	                    if(selectedPlayers[currentPlayer]==depthCharts[currentDepthChart][2])		
	                      {
	                      alreadySelected=true;	
	                      }	
	                    }
	                 if(!alreadySelected)
	                   {   
	                   if(selectedPlayers[selectedPlayerPosition-1]==-1)
	                     {	
	                     selectedPlayers[selectedPlayerPosition-1]=depthCharts[currentDepthChart][2];
	                     }
	                   else
	                     {
	                     currentDepthChart--;	  
	                     }
	                   if(doubledUp)
	                     {
				         selectedPlayerPosition++;
	                     doubledUp=false;	
	                     }
	                   else  	
	                     {
	                     currentDepthChart=depthCharts.length;
	                     playerFound=true;
	                     }
	                   }		
	                 }
	              }
			   if(!playerFound&&!positionFilled&&currentLoop==17)
			     {
			     if(chartEmpty)
			       {
				   Routines.writeToLog(servletName,"[" + fixtureNumber + "]" +
				                                   "loop=" + currentLoop + " " +
				                                   "formation=" + formation[0] + " " +
												   "formationPlayer[" + 
												   currentFormationPosition +
												   "]=" +
												   positionNumber +
												   " depth chart empty",false,context);
			       }
			     else
			       {  
				   Routines.writeToLog(servletName,"[" + fixtureNumber + "]" +
				                                   "loop=" + currentLoop + " " +
				                                   "formation=" + formation[0] + " " +
												   "formationPlayer[" + 
												   currentFormationPosition +
												   "]=" +
												   positionNumber +
												   " depth chart exhausted",false,context);
			       }  
			     }	  
			   if(defense&&(currentFormationPosition<6||currentFormationPosition==15)&&(!hasBeenDoubledUp||(currentLoop>0&&!playerFound&&hasBeenDoubledUp&&selectedPlayers[selectedPlayerPosition-1]!=-1)))
	             {  
			     selectedPlayerPosition++;
	             }
		       }
		     else
		       {
		       if(defense&&(currentFormationPosition<6||currentFormationPosition==15))	
		          {  
		          selectedPlayerPosition++;
		          }	  
		       }
	         }
		  int positionsFilled=0;	  
		  for(int currentFormationPosition=0;currentFormationPosition<selectedPlayers.length;currentFormationPosition++)
			 {
			 if(selectedPlayers[currentFormationPosition]!=-1)
			   {
			   positionsFilled++;	
			   }
			 }	 			  	  
	      if(positionsFilled==11)
	        {
	        allPositionsFound=true; 
	        }
	      if(!allPositionsFound&&currentLoop==17)
	        {  
			Routines.writeToLog(servletName,"[" + fixtureNumber + "] getPlayers Exhausted",false,context);
	        allPositionsFound=true;	
	        }
	   	  }   
		if(defense)
		  {
		  //Switch back RCB and Nickel/Dime back after forcing routine to give RCB the best CB.
		  int tempNickel=selectedPlayers[19];
		  int tempDime=selectedPlayers[20];
		  selectedPlayers[20]=selectedPlayers[16];
		  selectedPlayers[19]=selectedPlayers[18];
		  selectedPlayers[18]=selectedPlayers[17];
		  selectedPlayers[17]=tempDime;
		  selectedPlayers[16]=tempNickel;
		  }
	   for(int currentFormationPosition=0;currentFormationPosition<selectedPlayers.length;currentFormationPosition++)
		  {
		  if(selectedPlayers[currentFormationPosition]!=-1)
		    {
		    boolean found=false;	
		    for(int currentPlayer=0;currentPlayer<players.length;currentPlayer++)
		       {
		       if(selectedPlayers[currentFormationPosition]==players[currentPlayer][0])
		         {
		         selectedPlayers[currentFormationPosition]=currentPlayer;
		         found=true;
		         currentPlayer=players.length;					
		         }
		       }
		    if(!found)
		      {
			  Routines.writeToLog(servletName,"FixtureNumber=" + fixtureNumber + " " +
                                              "selectedPlayer[" + 
			                                  currentFormationPosition +
			                                  "]=" +
                              				  selectedPlayers[currentFormationPosition] +
			                                  " not found",false,context);	
			  selectedPlayers[currentFormationPosition]=-1;	
		      }	   
		    }
		  }
	   for(int currentPlayer=0;currentPlayer<selectedPlayers.length;currentPlayer++)
	      {	  
		  int numOfOccurances=0;
		  if(selectedPlayers[currentPlayer]!=-1)
		    {
		    for(int currentPlayer2=0;currentPlayer2<selectedPlayers.length;currentPlayer2++)
		       {	
		       if(selectedPlayers[currentPlayer]==selectedPlayers[currentPlayer2])
		         {
		         numOfOccurances++;		
		         }
		       }
		    if(numOfOccurances>1)
		      {
		      System.out.println("Player found twice in formation("+formation[0]+") fixture("+fixtureNumber+")");
 		      }
		    }  
	      }
	   return selectedPlayers;
	   }

      private playObject nextCard(playObject playData,
                                  int[][] actionCards,
                                  Random random)
       {
       int[]cardData=playData.getCardData();
       int[][]shuffledActionCards=playData.getShuffledActionCards();	
       int currentCard=cardData[0];
	   boolean realCardFound=false;
	   while(!realCardFound)
	     {
		 currentCard++;	
	     if(currentCard>=shuffledActionCards.length)
		   {
		   playData=shufflerTron(actionCards,random,playData);	
		   shuffledActionCards=playData.getShuffledActionCards();
		   cardData[0]=0;
		   currentCard=0;	
		   }
		 if(shuffledActionCards[currentCard][0]==0)
		   {
		   if(cardData[3]<zValidFor)
		     {	
		     cardData[1]++;	
		     cardData[2]++;
		     }
		   }
		 else
		   {
		   realCardFound=true;	  
		   }
	     }  
	   cardData[0]=currentCard;
	   cardData[3]++;
	   playData.setCardData(cardData);
	   if(shuffledActionCards[currentCard][0]==0)
	     {
	     System.out.println("CP1="+currentCard);	
	     }
       return playData;	 		 	
       }

    private int getSkill(boolean rawSkill,
                         int player,
                         int players[][],
                         int skillNumber,
                         int item,
                         boolean rating)
       {
	   boolean found=false;	
       int skill=0;
       int positionNumber=0;
       if(rawSkill)
         {
         skill=item;	
         item=1;
         }
       else
         {  
         positionNumber=players[player][1];
         for(int currentSkill=0;currentSkill<positionSkills.length;currentSkill++)
            {
		    if(positionSkills[currentSkill][0]==positionNumber&&
               positionSkills[currentSkill][1]==skillNumber)
               {
               skill=players[player][13+positionSkills[currentSkill][2]];
			   found=true;
               currentSkill=positionSkills.length;		
               }
            }
	     if(!found)
		   {
		   Routines.writeToLog(servletName,"Fixture("+fixtureNumber+"), no skill("+ skillNumber + ") for PositionNumber ("+positionNumber+")",false,context);
		   }
         }  
	   if(rating)
	     {	 
	     found=false;	
	     for(int currentSkill=0;currentSkill<skills.length;currentSkill++)
	        {
	        if(skills[currentSkill][0]==skillNumber&&
	           skill>=skills[currentSkill][1])
	           {
	           skill=skills[currentSkill][item+1];
	           found=true;
	           currentSkill=skills.length;			              
	           }
	        }
	     if(!found)
		   {
		   Routines.writeToLog(servletName,"Fixture("+fixtureNumber+"), no rating("+ skillNumber + ") for PositionNumber ("+positionNumber+") rating ("+skill+")",false,context);
		   }
	     }  	      
       return skill;	                    
       }

    private int specialTeamsModifier(boolean kickOff,
                                     boolean punt,
                                     boolean fieldGoal,
                                     int[]   kickOffChosenPlayers,
                                     int[]   kickDefChosenPlayers,
                                     int[][] kickOffPlayers,
                                     int[][] kickDefPlayers)
       {
       int returnInt=0;
       if(kickOff)
         {
         for(int currentPlayer=0;currentPlayer<kickOffChosenPlayers.length;currentPlayer++)
            {
            if(currentPlayer!=10&&kickOffChosenPlayers[currentPlayer]!=-1)
              {	
              int specialTeamSkill[]=specialTeamSkill(kickOffPlayers[kickOffChosenPlayers[currentPlayer]][1]);
              if(specialTeamSkill[1]!=0)
                {
			    int modifier=getSkill(false,kickOffChosenPlayers[currentPlayer],kickOffPlayers,specialTeamSkill[0],1,true);
			    modifier=modifier*specialTeamSkill[1];
			    returnInt+=modifier;
                }
              }
            }
		 for(int currentPlayer=0;currentPlayer<kickDefChosenPlayers.length;currentPlayer++)
			{
			if(currentPlayer!=10&&kickDefChosenPlayers[currentPlayer]!=-1)
			  {		
			  int specialTeamSkill[]=specialTeamSkill(kickDefPlayers[kickDefChosenPlayers[currentPlayer]][1]);
			  if(specialTeamSkill[1]!=0)
				{
			    int modifier=getSkill(false,kickDefChosenPlayers[currentPlayer],kickDefPlayers,specialTeamSkill[0],1,true);
			    modifier=modifier*specialTeamSkill[1];
			    returnInt-=modifier;
			    }
			  }  
			}
         }	
	   if(fieldGoal)
		  {
		  for(int currentPlayer=0;currentPlayer<12;currentPlayer++)
			 {
			 if(kickOffChosenPlayers[currentPlayer]!=-1)
			   {		
			   int specialTeamSkill[]=specialTeamSkill(kickOffPlayers[kickOffChosenPlayers[currentPlayer]][1]);
			   if(specialTeamSkill[1]!=0)
				 {
				 int modifier=getSkill(false,kickOffChosenPlayers[currentPlayer],kickOffPlayers,specialTeamSkill[0],1,true);
				 modifier=modifier*specialTeamSkill[1];
				 returnInt+=modifier;
				 }
			   }
			 }
		  double temp=returnInt;
		  temp/=9;
		  temp*=10;
		  returnInt=(int)temp;	 
		  for(int currentPlayer=0;currentPlayer<10;currentPlayer++)
			 {
			 if(kickDefChosenPlayers[currentPlayer]!=-1)
			   {		
			   int specialTeamSkill[]=specialTeamSkill(kickDefPlayers[kickDefChosenPlayers[currentPlayer]][1]);
			   if(specialTeamSkill[1]!=0)
				 {
				 int modifier=getSkill(false,kickDefChosenPlayers[currentPlayer],kickDefPlayers,specialTeamSkill[0],1,true);
				 modifier=modifier*specialTeamSkill[1];
				 if(modifier<0)
				   {
				   modifier=0;	
				   }
				 returnInt-=modifier;
				 }
			   }  
			 }
        }
		if(punt)
		   {
		   for(int currentPlayer=0;currentPlayer<13;currentPlayer++)
			  {
			  if(kickOffChosenPlayers[currentPlayer]!=-1)
				{		
				int specialTeamSkill[]=specialTeamSkill(kickOffPlayers[kickOffChosenPlayers[currentPlayer]][1]);
				if(specialTeamSkill[1]!=0)
				  {
				  int modifier=getSkill(false,kickOffChosenPlayers[currentPlayer],kickOffPlayers,specialTeamSkill[0],1,true);
				  modifier=modifier*specialTeamSkill[1];
				  returnInt+=modifier;
				  }
				}
			  }
		   for(int currentPlayer=0;currentPlayer<15;currentPlayer++)
			  {
			  if(kickDefChosenPlayers[currentPlayer]!=-1)
				{		
				int specialTeamSkill[]=specialTeamSkill(kickDefPlayers[kickDefChosenPlayers[currentPlayer]][1]);
				if(specialTeamSkill[1]!=0)
				  {
				  int modifier=getSkill(false,kickDefChosenPlayers[currentPlayer],kickDefPlayers,specialTeamSkill[0],1,true);
				  modifier=modifier*specialTeamSkill[1];
				  if(modifier<0)
					{
					modifier=0;	
					}
				  returnInt-=modifier;
				  }
				}  
			  }
		 if(returnInt>-6&&returnInt<6)
		   {
		   returnInt=0;		  
		   }
         if(returnInt<-5)
           {
           returnInt=-1;	
           }
         if(returnInt>5)
           {
           returnInt=1;	  
           }
		 }        
       return returnInt;
       }
       
	private int[] specialTeamsTackler(boolean kickOff,
	                                  boolean onside,
	                                  boolean onsideRecovered,
									  boolean punt,
									  boolean breakOut,
									  int[]   kickOffChosenPlayers,
									  int[][] kickOffPlayers,
	                                  Random random)
	   {
	   int returnInt[]=new int[4];
	   returnInt[0]=0;//Primary tackler
	   returnInt[1]=0;//Secondary tackler
	   returnInt[2]=0;//OnsideKickLocation
	   int tackling[]=new int[kickOffChosenPlayers.length];
	   int totalTackling=0;
	   if(kickOff)
		 {
		 for(int currentPlayer=0;currentPlayer<kickOffChosenPlayers.length;currentPlayer++)
			{
			if((currentPlayer!=10||breakOut)&&kickOffChosenPlayers[currentPlayer]!=-1)
			  {		
			  int specialTeamSkill[]=new int[3];
			  if(currentPlayer!=10)
			    {	
			    specialTeamSkill=specialTeamSkill(kickOffPlayers[kickOffChosenPlayers[currentPlayer]][1]);
			    }
			  if(specialTeamSkill[1]!=0)
				{
				if(currentPlayer==10)
				  {	
				  tackling[currentPlayer]=1;	
				  }
				else
				  {  
				  tackling[currentPlayer]=getSkill(false,kickOffChosenPlayers[currentPlayer],kickOffPlayers,specialTeamSkill[0],1,false);
				  }
				}
			  }
			if(kickOffChosenPlayers[currentPlayer]!=-1)
			  {   	
			  if(tackling[currentPlayer]==0)
			    {
			    tackling[currentPlayer]=1;		
			    }
			  totalTackling+=tackling[currentPlayer];  
			  }
			}
		 }
	   if(punt)
		 {
		 for(int currentPlayer=0;currentPlayer<kickOffChosenPlayers.length;currentPlayer++)
			{
			if((currentPlayer!=14||breakOut)&&kickOffChosenPlayers[currentPlayer]!=-1)
			  {		
			  int specialTeamSkill[]=new int[3];
			  if(currentPlayer!=14)
			    {	
			    specialTeamSkill=specialTeamSkill(kickOffPlayers[kickOffChosenPlayers[currentPlayer]][1]);
			    }
			  if(specialTeamSkill[1]!=0)
				{
				if(currentPlayer==14)
				  {	
				  tackling[currentPlayer]=1;	
			      }
				else
				  {  
				  tackling[currentPlayer]=getSkill(false,kickOffChosenPlayers[currentPlayer],kickOffPlayers,specialTeamSkill[0],1,false);
				  }
				if(currentPlayer==10||currentPlayer==11)
				  {
				  //Increase chance of gunners getting tackle	
				  tackling[currentPlayer]+=10;	
				  }
				}
			  }
			if(kickOffChosenPlayers[currentPlayer]!=-1)
			  {   	
			  if(tackling[currentPlayer]==0)
				{
				tackling[currentPlayer]=1;		
				}
			  totalTackling+=tackling[currentPlayer];  
			  }
			}
		 }		 
       int numOfTacklers=1+random.nextInt(2);
       numOfTacklers++;
       for(int currentTackler=0;currentTackler<numOfTacklers;currentTackler++)
          {
          int tackleNumber=random.nextInt(totalTackling)+1;
          int tackler=0;	 	 
	      for(int currentPlayer=0;currentPlayer<kickOffChosenPlayers.length;currentPlayer++)
		     {
             tackleNumber-=tackling[currentPlayer];
             if(tackleNumber<=0||currentPlayer==kickOffChosenPlayers.length-1)
               {
               returnInt[currentTackler]=kickOffPlayers[kickOffChosenPlayers[currentPlayer]][0];
               if(currentTackler==0&&onside)
                 {
			     boolean right=false;
			     for(int currentRighty=0;currentRighty<onsideRightRecoverers.length;currentRighty++)
			        {
			        if(currentPlayer==onsideRightRecoverers[currentRighty])
			          {
			          right=true;			
			          }
			        }
			     if(currentPlayer>4||(onsideRecovered&&right))
			       {
			       returnInt[2]=1;	
			       }
                 }
               currentPlayer=kickOffChosenPlayers.length;			  	
               }
		     }
          }  	   	 
	   if(returnInt[0]==returnInt[1]||onside)
		 {
		 returnInt[1]=0;			 
		 }
	   return returnInt;	
	   } 
	   
	private int whoBlockedOrRecoveredTheKick(boolean kickOff,
	                                         boolean fieldGoal,
	                                         boolean punt,
	                                         boolean blocked,
	                                         int[]   chosenPlayers,
								             int[][] players,
								             Random random)
	   {
	   int returnInt=0;
	   int totalCount=0;
	   int totals[]=null;
	   if(blocked)
	     {
	     totals=new int[10];
	     for(int currentPlayer=0;currentPlayer<10;currentPlayer++)
		    {
		    if(chosenPlayers[currentPlayer]!=-1)
			  {		
			  int specialTeamSkill[]=specialTeamSkill(players[chosenPlayers[currentPlayer]][1]);
              if(specialTeamSkill[1]!=0)
			    {
			    totals[currentPlayer]=getSkill(false,chosenPlayers[currentPlayer],players,specialTeamSkill[0],1,false);
			    totalCount+=totals[currentPlayer];
			    }
			  }
            }
	     }
	   else
	     {
		 totals=new int[chosenPlayers.length];
		 for(int currentPlayer=0;currentPlayer<chosenPlayers.length;currentPlayer++)
		 	{
			if(chosenPlayers[currentPlayer]!=-1)
			  {		
			  int specialTeamSkill[]=new int[3];
			  if((kickOff&&currentPlayer!=10)||(fieldGoal&&(currentPlayer!=13&&currentPlayer!=14))||(punt&&currentPlayer!=14))
				{	
				specialTeamSkill=specialTeamSkill(players[chosenPlayers[currentPlayer]][1]);
				if(specialTeamSkill[1]!=0)
				  {
				  totals[currentPlayer]=getSkill(false,chosenPlayers[currentPlayer],players,specialTeamSkill[0],1,false);
				  totalCount+=totals[currentPlayer];
				  }
				}
			  else
			    { 
				totalCount+=2;	   
				}
			  }			 		
			}
	     }
	   int totalNumber=random.nextInt(totalCount+1);
	   for(int currentPlayer=0;currentPlayer<chosenPlayers.length;currentPlayer++)
		  {
		  totalNumber-=totals[currentPlayer];
		  if(totalNumber<=0)
			 {
			 returnInt=players[chosenPlayers[currentPlayer]][0];
		     currentPlayer=chosenPlayers.length;			  	
			 }
		  }
	   return returnInt;	
	   }	         

    private int[] specialTeamSkill(int positionNumber)
       {
       int returnInt[]=new int[2];
       if(positionNumber>=7&&positionNumber<=11)
         {
         //OL	
		 returnInt[0]=55;
		 returnInt[1]=1;	
         }
       if(positionNumber==27||positionNumber==26||positionNumber==21)
         {
         //DB	
		 returnInt[0]=59; 
		 returnInt[1]=-1; 
         }
       if(positionNumber==12)
         {
         //TE
		 returnInt[0]=53;
		 returnInt[1]=1;	  
         }
       if(positionNumber==13)
         {
         //WR	
		 returnInt[0]=54;
		 returnInt[1]=1;  
         }
       if(positionNumber==15)
         {
         //RB	
         returnInt[0]=43;
		 returnInt[1]=1;
         }  
       if(positionNumber==17||positionNumber==18)
         {
         //DL
		 returnInt[0]=59;
		 returnInt[1]=-1; 	
         }	 
	   if(positionNumber==19||positionNumber==20)
		 {
		 //LB	
		 returnInt[0]=59;
		 returnInt[1]=-1;
		 }	       
	   if(returnInt[0]==0)
	     {
		 Routines.writeToLog(servletName,"[specialTeamSkill][FixtureNumber="+fixtureNumber+"]Position Not Found : " + positionNumber,false,context);	
	     }
       return returnInt;	
       }

    private boolean amIBlitzing(int defender,int[] defensiveSelectedPlayers,boolean[] blitzers)
       {
       boolean amI=false;
	   if(defender==defensiveSelectedPlayers[10]&&blitzers[0])
	     {
         //OLLB	
	     amI=true;
	     System.out.println("CP1");
	     }	
	   if(defender==defensiveSelectedPlayers[11]&&blitzers[1])
		 {
		 //ILLB	
		 amI=true;
		 System.out.println("CP2");
		 }	
	   if(defender==defensiveSelectedPlayers[12]&&blitzers[2])
		 {
		 //MLB	
		 amI=true;
		 System.out.println("CP3");
		 }			    
	   if(defender==defensiveSelectedPlayers[13]&&blitzers[3])
		 {
		 //IRLB	
		 amI=true;
		 System.out.println("CP4");
		 }	
	   if(defender==defensiveSelectedPlayers[14]&&blitzers[4])
		 {
		 //OLLB	
		 amI=true;
		 System.out.println("CP5");
		 }	
	   if(defender==defensiveSelectedPlayers[15]&&blitzers[5])
		 {
		 //LCB	
		 amI=true;
		 System.out.println("CP6");
		 }	
	   if(defender==defensiveSelectedPlayers[16]&&blitzers[6])
		 {
		 //Nickel Back	
		 amI=true;
		 System.out.println("CP7");
		 }				    
	   if(defender==defensiveSelectedPlayers[17]&&blitzers[10])
		 {
		 //Dime Back	
		 amI=true;
		 System.out.println("CP8");
		 }	
	   if(defender==defensiveSelectedPlayers[18]&&blitzers[7])
		 {
		 //FS	
		 amI=true;
		 System.out.println("CP9");
		 }	
	   if(defender==defensiveSelectedPlayers[19]&&blitzers[8])
		 {
		 //SS	
		 amI=true;
		 }				
	   if(defender==defensiveSelectedPlayers[20]&&blitzers[9])
		 {
		 //FS	
		 amI=true;
		 System.out.println("CP11");
		 }	
       return amI;	
       }

	private int getBlockingSkill(int positionNumber,boolean run)
	   {
	   int returnInt=0;
	   if(positionNumber>=7&&positionNumber<=11)
		 {
		 //OL	
		 if(run)
		   {
		   returnInt=55;
		   }
		 else
		   {
		   returnInt=56;	  
		   }
		 }
	   if(positionNumber==27||positionNumber==26||positionNumber==21)
		 {
		 //DB	
		 if(run)
		   {
		   returnInt=59;
		   }
		 else
		   {
		   returnInt=58;	  
		   }
		 }
	   if(positionNumber==12)
		 {
		 //TE
		 returnInt=53;
		 }
	   if(positionNumber==13)
		 {
		 //WR	
		 returnInt=54;
		 }
	   if(positionNumber==15)
		 {
		 //RB	
		 returnInt=43;
		 }  
	   if(positionNumber==17||positionNumber==18)
		 {
		 //DL
		 if(run)
		   {
		   returnInt=59;
		   }
		 else
		   {
		   returnInt=58;	  
		   }
		 }	 
	   if(positionNumber==19||positionNumber==20)
		 {
		 //LB	
		 if(run)
		   {
		   returnInt=59;
		   }
		 else
		   {
		   returnInt=58;	  
		   }
		 }	       
	   if(returnInt==0)
		 {
		 Routines.writeToLog(servletName,"["+fixtureNumber+"][getBlockingSkill] Position Not Found : " + positionNumber,false,context);	
		 }
	   return returnInt;	
	   }

	private int getDeflectSkill(int positionNumber)
	   {
	   boolean found=false;	
	   int returnInt=0;
	   if(positionNumber>=7&&positionNumber<=11)
		 {
		 //OL
		 found=true;	
		 }
	   if(positionNumber==27||positionNumber==26||positionNumber==21)
		 {
		 //DB	
		 found=true;
		 }
	   if(positionNumber==12)
		 {
		 //TE
		 found=true;
		 }
	   if(positionNumber==13)
		 {
		 //WR	
		 found=true;
		 }
	   if(positionNumber==14)
		 {
		 //QB	
		 found=true;
		 }		 
	   if(positionNumber==15)
		 {
		 //RB	
		 found=true;
		 }  
	   if(positionNumber==17||positionNumber==18)
		 {
		 //DL
		 returnInt=62;
		 found=true;
		 }	 
	   if(positionNumber==19||positionNumber==20)
		 {
		 //LB	
		 found=true;
		 }	       
	   if(!found)
		 {
		 Routines.writeToLog(servletName,"[getDeflectSkill] Position Not Found : " + positionNumber,false,context);	
		 }
	   return returnInt;	
	   }


	private int[] getRunningSkill(int positionNumber)
	   {
	   boolean found=false;	
	   int returnInt[]=new int[4];
	   if(positionNumber>=7&&positionNumber<=11)
		 {
		 //OL
		 found=true;	
		 }
	   if(positionNumber==27||positionNumber==26||positionNumber==21)
		 {
		 //DB	
		 found=true;
		 }
	   if(positionNumber==12)
		 {
		 //TE
		 returnInt[0]=44;
		 returnInt[1]=44;
		 returnInt[2]=45;
		 returnInt[3]=46;
		 found=true;
		 }
	   if(positionNumber==13)
		 {
		 //WR	
		 returnInt[0]=70;
		 returnInt[1]=70;
		 returnInt[2]=71;
		 returnInt[3]=72;
		 found=true;
		 }
		if(positionNumber==14)
		  {
		  //QB	
		  returnInt[0]=33;
		  returnInt[1]=66;
		  returnInt[2]=34;
		  returnInt[3]=35;
		  found=true;
		  }		 
	   if(positionNumber==15)
		 {
		 //RB	
		 returnInt[0]=36;
		 returnInt[1]=37;
		 returnInt[2]=38;
		 returnInt[3]=39;
		 found=true;
		 }  
	   if(positionNumber==17||positionNumber==18)
		 {
		 //DL
		 found=true;
		 }	 
	   if(positionNumber==19||positionNumber==20)
		 {
		 //LB	
		 found=true;
		 }	       
	   if(!found)
		 {
		 Routines.writeToLog(servletName,"[getRunningSkill] Position Not Found : " + positionNumber,false,context);	
		 }
	   return returnInt;	
	   }
	private int[] getReceivingSkill(int positionNumber)
	   {
	   boolean found=false;	
	   int returnInt[]=new int[3];
	   if(positionNumber>=7&&positionNumber<=11)
		 {
		 //OL
		 found=true;	
		 }
	   if(positionNumber==27||positionNumber==26||positionNumber==21)
		 {
		 //DB	
		 found=true;
		 }
	   if(positionNumber==12)
		 {
		 //TE
		 returnInt[0]=47;
		 returnInt[1]=48;
		 returnInt[2]=49;
		 found=true;
		 }
	   if(positionNumber==13)
		 {
		 //WR	
		 returnInt[0]=50;
		 returnInt[1]=51;
		 returnInt[2]=52;
		 found=true;
		 }
		if(positionNumber==14)
		  {
		  //QB	
		  found=true;
		  }		 
	   if(positionNumber==15)
		 {
		 //RB	
		 returnInt[0]=40;
		 returnInt[1]=41;
		 returnInt[2]=42;
		 found=true;
		 }  
	   if(positionNumber==17||positionNumber==18)
		 {
		 //DL
		 found=true;
		 }	 
	   if(positionNumber==19||positionNumber==20)
		 {
		 //LB	
		 found=true;
		 }	       
	   if(!found)
		 {
		 Routines.writeToLog(servletName,"[getReceivingSkill] Position Not Found : " + positionNumber,false,context);	
		 }
	   return returnInt;	
	   }

	private int[] getPassingSkills(int positionNumber)
	   {
	   boolean found=false;	
	   int returnInt[]=new int[6];
	   if(positionNumber>=7&&positionNumber<=11)
		 {
		 //OL
		 found=true;	
		 }
	   if(positionNumber==27||positionNumber==26||positionNumber==21)
		 {
		 //DB	
		 found=true;
		 }
	   if(positionNumber==12)
		 {
		 //TE
		 found=true;
		 }
	   if(positionNumber==13)
		 {
		 //WR	
		 found=true;
		 }
	   if(positionNumber==14)
		 {
		 //QB	
		 returnInt[0]=29;
		 returnInt[1]=63;
		 returnInt[2]=30;
		 returnInt[3]=64;
		 returnInt[4]=31;
		 returnInt[5]=65;
		 found=true;
		 }		 
	   if(positionNumber==15)
		 {
		 //RB	
		 found=true;
		 }  
	   if(positionNumber==17||positionNumber==18)
		 {
		 //DL
		 found=true;
		 }	 
	   if(positionNumber==19||positionNumber==20)
		 {
		 //LB	
		 found=true;
		 }	       
	   if(!found)
		 {
		 Routines.writeToLog(servletName,"[getPassingSkills] Position Not Found : " + positionNumber,false,context);	
		 }
	   return returnInt;	
	   }

	private int[] getCoverSkills(int positionNumber)
	   {
	   boolean found=false;	
	   int returnInt[]=new int[2];
	   if(positionNumber>=7&&positionNumber<=11)
		 {
		 //OL
		 found=true;	
		 }
	   if(positionNumber==27||positionNumber==26||positionNumber==21)
		 {
		 //DB	
		 found=true;
		 returnInt[0]=57;
		 returnInt[1]=60;
		 }
	   if(positionNumber==12)
		 {
		 //TE
		 found=true;
		 }
	   if(positionNumber==13)
		 {
		 //WR	
		 found=true;
		 }
	   if(positionNumber==14)
		 {
		 //QB	
		 found=true;
		 }		 
	   if(positionNumber==15)
		 {
		 //RB	
		 found=true;
		 }  
	   if(positionNumber==17||positionNumber==18)
		 {
		 //DL
		 found=true;
		 }	 
	   if(positionNumber==19)
		 {
		 //MLB	
		 found=true;
		 returnInt[0]=57;
		 returnInt[1]=60;
		 }	       
	   if(positionNumber==20)
		 {
		 //OLB	
		 found=true;
		 returnInt[0]=57;
		 returnInt[1]=60;		 
		 }	       
	   if(!found)
		 {
		 Routines.writeToLog(servletName,"[getPassDefenseSkill] Position Not Found : " + positionNumber,false,context);	
		 }
	   return returnInt;	
	   }
	   
	private int[] getKickingSkills(int positionNumber)
	   {
	   boolean found=false;	
	   int returnInt[]=new int[15];
	   if(positionNumber==1)
		 {
		 //K
		 found=true;	
		 returnInt[0]=4;
		 returnInt[1]=67;
		 returnInt[2]=26;
		 returnInt[3]=5;
		 returnInt[4]=6;
		 returnInt[5]=7;
		 returnInt[6]=8;
		 returnInt[7]=10;
		 returnInt[8]=11;
		 returnInt[9]=12;
		 returnInt[10]=13;
		 returnInt[11]=14;
		 returnInt[12]=16;
		 returnInt[13]=17;
		 returnInt[14]=77;
		 }
	   if(positionNumber==2)
		 {
		 //P
		 found=true;	
		 }
	   if(positionNumber>=7&&positionNumber<=11)
		 {
		 //OL
		 found=true;	
		 }
	   if(positionNumber==27||positionNumber==26||positionNumber==21)
		 {
		 //DB	
		 found=true;
		 }
	   if(positionNumber==12)
		 {
		 //TE
		 found=true;
		 }
	   if(positionNumber==13)
		 {
		 //WR	
		 found=true;
		 }
		if(positionNumber==14)
		  {
		  //QB	
		  found=true;
		  }		 
	   if(positionNumber==15)
		 {
		 //RB	
		 found=true;
		 }  
	   if(positionNumber==17||positionNumber==18)
		 {
		 //DL
		 found=true;
		 }	 
	   if(positionNumber==19||positionNumber==20)
		 {
		 //LB	
		 found=true;
		 }	       
	   if(!found)
		 {
		 Routines.writeToLog(servletName,"[getKickingSkills] Position Not Found : " + positionNumber,false,context);	
		 }
	   return returnInt;	
	   }	   
	   
	private int[] getPuntingSkills(int positionNumber)
	   {
	   boolean found=false;	
	   int returnInt[]=new int[3];
	   if(positionNumber==1)
		 {
		 //K
		 found=true;	
		 }
	   if(positionNumber==2)
		 {
		 //P
		 found=true;	
		 returnInt[0]=19;
		 returnInt[1]=21;
		 returnInt[2]=69;
		 }
	   if(positionNumber>=7&&positionNumber<=11)
		 {
		 //OL
		 found=true;	
		 }
	   if(positionNumber==27||positionNumber==26||positionNumber==21)
		 {
		 //DB	
		 found=true;
		 }
	   if(positionNumber==12)
		 {
		 //TE
		 found=true;
		 }
	   if(positionNumber==13)
		 {
		 //WR	
		 found=true;
		 }
		if(positionNumber==14)
		  {
		  //QB	
		  found=true;
		  }		 
	   if(positionNumber==15)
		 {
		 //RB	
		 found=true;
		 }  
	   if(positionNumber==17||positionNumber==18)
		 {
		 //DL
		 found=true;
		 }	 
	   if(positionNumber==19||positionNumber==20)
		 {
		 //LB	
		 found=true;
		 }	       
	   if(!found)
		 {
		 Routines.writeToLog(servletName,"[getPuntingSkills] Position Not Found : " + positionNumber,false,context);	
		 }
	   return returnInt;	
	   }	   	   

    private int[] zCardProcess(playObject playData,
                               int[][] actionCards,
                               Random random,
                               int yardage)
       {
       int[] returnInt=new int[5];
       int[][] shuffledActionCards=playData.getShuffledActionCards();
       int[] cardData=playData.getCardData();
       boolean injuryAlreadyOccured=false;
       boolean penaltyAlreadyOccured=false;
       boolean fumbleAlreadyOccured=false;
       returnInt[0]=cardData[0];
       returnInt[2]=cardData[2];
       returnInt[4]=yardage;
       for(int currentZ=0;currentZ<numOfZPerPlay&&currentZ<cardData[1];currentZ++)
       	{	
		playData=nextCard(playData,actionCards,random);
		shuffledActionCards=playData.getShuffledActionCards();
		cardData=playData.getCardData();	
       	if(cardData[2]>=20)
          {
		  playData=nextCard(playData,actionCards,random);
		  shuffledActionCards=playData.getShuffledActionCards();
		  cardData=playData.getCardData();
		  int runNumber=shuffledActionCards[cardData[0]][1];
		  returnInt[2]=0;
		  if(runNumber<9)
		    {
		    returnInt[4]*=2;	
		    }
		  else
		    {
		    returnInt[4]*=3;		  
		    }
          }
         else
          { 
          int injury=shuffledActionCards[cardData[0]][sizeOfCardArray-7]; 
		  int fumble=shuffledActionCards[cardData[0]][sizeOfCardArray-6]; 
		  int penalty1=shuffledActionCards[cardData[0]][sizeOfCardArray-5];
		  int penalty2=shuffledActionCards[cardData[0]][sizeOfCardArray-4];
		  int penalty3=shuffledActionCards[cardData[0]][sizeOfCardArray-3];  
          int penalty4=shuffledActionCards[cardData[0]][sizeOfCardArray-2];
          boolean zFound=false;
          if(injury!=0)
            {
            if(!injuryAlreadyOccured)
              {	
              System.out.println("Z("+fixtureNumber+") : Injury " + injury);
              injuryAlreadyOccured=true;
              }	
			zFound=true;  
            }
		  if(fumble!=0)
			{
			if(!fumbleAlreadyOccured)
		      {		
		      System.out.println("Z("+fixtureNumber+") : Fumble" + fumble);	
			  fumbleAlreadyOccured=true;
		      }  
			zFound=true;
			}
		  if(penalty1!=0||penalty2!=0||penalty3!=0||penalty4!=0)
		    {
		    if(!penaltyAlreadyOccured)
		      {	
			  System.out.println("Z("+fixtureNumber+") : Penalty" + penalty1 + " " + penalty2 + " " + penalty3 + " " + penalty4);
			  penaltyAlreadyOccured=true;			      	
		      }
			zFound=true;  
			}
		  if(!zFound)
		    {
		    System.out.println("No Z("+fixtureNumber+") Found");	
			Routines.writeToLog(servletName,"No Z Found",false,context);	            
		    }
          }
       	}
       return returnInt;  
       }

    private playObject shufflerTron(int[][] actionCards,
                                    Random random,
                                    playObject playData)
       {
       int currentCard=0;	
       int[][] shuffledActionCards=new int[actionCards.length][sizeOfCardArray-1];
		  for(int currentLength=shuffledActionCards.length;currentLength>0;currentLength--)
			 {
			 int cardNumber=random.nextInt(currentLength);
			 int countCard=0;
			 for(int checkCard=0;checkCard<actionCards.length;checkCard++)
				{
				if(actionCards[checkCard][0]==0)
				  {
				  if(countCard==cardNumber)
					{
					actionCards[checkCard][0]=1;			
					for(int currentItem=0;currentItem<(sizeOfCardArray-1);currentItem++)
					   {
					   shuffledActionCards[currentCard][currentItem]=actionCards[checkCard][currentItem+1];		
					   }
					currentCard++;
					checkCard=actionCards.length;	
					}
				  else
					{
					countCard++;	  
					}
				  }
				}
			 }
	    for(currentCard=0;currentCard<actionCards.length;currentCard++)
		   {
		   actionCards[currentCard][0]=0;		 	
		   }
	   playData.setActionCards(shuffledActionCards);	   
       return playData;    	
       }

    private playObject playRunner(int ballOn,
                                  int toGo, 
                                  int homeTeam,
                                  int awayTeam,
                                  int[] homePlay,
                                  int[] awayPlay,
                                  int homeFormation,
                                  int awayFormation,
                                  int[] homeSelectedPlayers,
                                  int[] awaySelectedPlayers,
                                  int[][] homePlayers,
                                  int[][] awayPlayers,
	                              int[] homeCoverRatings,
								  int[] awayCoverRatings,
                                  boolean homeTeamInPossession,
                                  boolean extraPointRequired,
                                  playObject playData,
                                  int[][] actionCards,
                                  Connection database,
	                              Random random)
	   {
	   int playResult[]=new int[64];
	   int[][]shuffledActionCards=playData.getShuffledActionCards();
	   int[]cardData=playData.getCardData();
	   //playResult[0]=initialBallCarrier
	   //playResult[1]=secondaryBallCarrier
	   //playResult[2]=tacklers[0]
	   //playResult[3]=tacklers[1]
	   //playResult[4]=recoverer
	   //playResult[5]=returner
	   //playResult[6]=offensiveYards
	   //playResult[7]=defensiveYards  
	   //playResult[8]=returnYards
	   //playResult[9]=animation1   //Blocker1(Run)//HangTime(Punt)//PrimaryReceviers(Pass)
	   //playResult[10]=animation2  //Blocker2(Run)//CoverageMod(Punt)//SecondaryReceiver(Pass)
	   //playResult[11]=animation3  //Tackler1(Run)//Bounce(Punt)//GreatRoute(Pass)
	   //playResult[12]=animation4  //Tackler2(Run)//BlanketCoverage(Pass)
	   //playResult[13]=animation5  //Blocker3(Run)
	   //playResult[14]=animation6  //Blocker4(Run)
	   //playResult[15]=animation7  //Tackler3(Run)
	   //playResult[16]=animation8  //Tackler4(Run)
	   //playResult[17]=animation9  //DoubleTeamPosition(Run)
       //playResult[18]=animation10 //PrimaryDoubleTeamer(Run)
       //playResult[19]=animation11 //SecondaryDoubleTeamer(Run)	
	   //playResult[20]=animation12 //BlockingWinner1(Run)
	   //playResult[21]=animation13 //BlockingValue1(Run)
	   //playResult[22]=animation14 //BlockingWinner2(Run)
	   //playResult[23]=animation15 //BlockingValue2(Run)
	   //playResult[24]=animation16 //WhichBlock(Run)
	   //playResult[25]=animation17 //CorrectChoice(Run)
       //playResult[26]=animation18 //Tackler5(Run)
       //playResult[27]=animation19 //Tackler6(Run)
       //playResult[28]=animation20 //Tackler7(Run)
       //playResult[29]=animation21 //Tackler8(Run)
	   //playResult[30]=fumbled
	   //playResult[31]=turnover
	   //playResult[32]=tacklers[2]
	   //playResult[33]=tacklers[3]
	   //playResult[34]=animation22 //Blocker5(Run)
	   //playResult[35]=animation23 //Blocker6(Run)
	   //playResult[36]=animation24 //Blocker7(Run)
	   //playResult[37]=animation25 //Blocker8(Run)
	   //playResult[38]=kickYards
	   //playResult[39]=forcer
	   //playResult[40]=outOfBounds
	   //playResult[41]=penalty
       //playResult[42]=animation26 
	   //playResult[43]=animation27 
	   //playResult[44]=animation28 
	   //playResult[45]=animation29 
	   //playResult[46]=animation30 
	   //playResult[47]=animation31 
	   //playResult[48]=animation32 
	   //playResult[49]=animation33 
	   //playResult[50]=animation34 
	   //playResult[51]=animation35 
       //playResult[52]=animation36 
	   //playResult[53]=animation37 
	   //playResult[54]=animation38 
	   //playResult[55]=animation39 //DoubleCoverageOn1(Run)//doubleCoverageOn1(Pass)
	   //playResult[56]=animation40 //DoubleCoverageOn2(Run)//doubleCoverageOn2(Pass)
       //playResult[57]=animation41 //FlipFloppedSafeties(Pass)
	   //playResult[58]=animation42 
	   //playResult[59]=animation43 
	   //playResult[60]=animation44 
	   //playResult[61]=animation45 
       //playResult[62]=description01 
	   //playResult[63]=description02 
	   boolean offensivePlayFound=false;
	   boolean defensivePlayFound=false;
	   int offensiveTeam=0;
	   int defensiveTeam=0;
	   int offensiveFormation=-1;
	   int defensiveFormation=-1;
	   int offensivePlay[]=null;
	   int defensivePlay[]=null;
	   int offensiveSelectedPlayers[]=null;
	   int defensiveSelectedPlayers[]=null;
	   int offensivePlayers[][]=null;
	   int defensivePlayers[][]=null;
	   int keyRunner=0;
	   int keyReceiver=0;
	   boolean keyRunnerFound=false;
	   boolean keyReceiverFound=false;
	   int receivers[]=null;
	   int coverageTable[][]=new int[5][5];
	   int[][] coverGuys=new int[11][3];
	   if(homeTeamInPossession)
	     {
	     offensiveTeam=homeTeam;
	     defensiveTeam=awayTeam;	
	     offensiveFormation=homeFormation;
	     defensiveFormation=awayFormation;	
	     offensivePlay=homePlay;
	     defensivePlay=awayPlay;
	     offensiveSelectedPlayers=homeSelectedPlayers;
	     defensiveSelectedPlayers=awaySelectedPlayers;
	     offensivePlayers=homePlayers;
	     defensivePlayers=awayPlayers;
	     keyRunner=awayCoverRatings[0];
	     keyReceiver=awayCoverRatings[1];
	     receivers=awayCoverRatings;
	     }
	   else
	     {  
	     offensiveTeam=awayTeam;
	     defensiveTeam=homeTeam;	
	     offensiveFormation=awayFormation;
	     defensiveFormation=homeFormation;	
	     offensivePlay=awayPlay;
	     defensivePlay=homePlay;	
	     offensiveSelectedPlayers=awaySelectedPlayers;
	     defensiveSelectedPlayers=homeSelectedPlayers;
	     offensivePlayers=awayPlayers;
	     defensivePlayers=homePlayers;
	     keyRunner=homeCoverRatings[0];
	     keyReceiver=homeCoverRatings[1];
	     receivers=homeCoverRatings;
	     }
	   //Create new receivers table containing only those players currently on the pitch.
	   int[] tempReceivers=new int[receivers.length];
	   int currentReceivers=0;
	   for(int currentReceiver=0;currentReceiver<receivers.length;currentReceiver++)
		  {
		  tempReceivers[currentReceiver]=-1;	
		  }		   
	   for(int currentReceiver=0;currentReceiver<receivers.length;currentReceiver++)
	      {
	      if(receivers[currentReceiver]!=-1)
	        {	
	        for(int currentPlayer=0;currentPlayer<offensiveSelectedPlayers.length;currentPlayer++)
	           {	
	           if(offensiveSelectedPlayers[currentPlayer]==receivers[currentReceiver])
	             {
	             tempReceivers[currentReceivers]=receivers[currentReceiver];	
	             currentReceivers++;
				 }
	           }
	        }         
	      }   
	   receivers=tempReceivers;   
	   boolean doubleTeamDefender=false;
	   boolean primaryDoubleTeamerReferenced=false;
	   boolean secondaryDoubleTeamerReferenced=false;
	   boolean doubleTeamReceiver=false;
	   boolean tripleTeamReceiver=false;
	   boolean doubleTeamReceiverX2=false;
	   int positionNumber=offensivePlay[3];
	   int playNumber=offensivePlay[4];
	   int defensivePlayNumber=defensivePlay[4];
	   int doubleTeamPosition=0;;
	   int primaryDoubleTeamer=0;
	   int secondaryDoubleTeamer=0;
	   int[][] blockers=new int[2][4];
	   int[][] tacklers=new int[2][4];
	   int currentMatchup=-1;
	   int currentTackler2=0;
	   int currentBlocker=0;
	   int passBlockTotal=0;
	   int passRushTotal=0;
	   int passDeflectTotal=0;
	   boolean keyFB1=false;
	   boolean keyFB2=false;
	   boolean keyRB1=false;
	   boolean keyRB2=false;
	   boolean keyQB=false;
	   boolean keyNone=false;
	   boolean passDefense=false;
	   boolean preventDefense=false;
	   boolean blitz=false;
	   boolean[] blitzers=new boolean[11];
	   boolean[] covering=new boolean[2];
	   boolean shotGun=false;
	   boolean flipFloppedSafeties=false;
	   boolean bootLeg=false;
	   boolean run=false;
	   boolean stunt1=false;
	   boolean stunt2=false;
	   boolean strongLeft=false;
	   boolean strongRight=false;
	   boolean stuntLeft=false;
	   boolean stuntRight=false;
	   int numOfFreeDB=-1;
	   int[] freeDB=new int[3];
	   int keyReceiverNumber=0;
	   //blitzers[0]=blitzOLLB;
	   //blitzers[1]=blitzILLB;
	   //blitzers[2]=blitzMLB;
	   //blitzers[3]=blitzIRLB;
	   //blitzers[4]=blitzORLB;
	   //blitzers[5]=blitzLCB;
	   //blitzers[6]=blitzNickel;
	   //blitzers[7]=blitzFS;
	   //blitzers[8]=blitzSS;
	   //blitzers[9]=blitzRCB;
	   //blitzers[10]=blitzDime;
	   if(offensivePlay[4]>=1&&offensivePlay[4]<=8)
	     {
	     run=true;	
	     }
	   if(formations[offensiveFormation][15]==14)
	     {
	     shotGun=true;	
	     }
	   if(defensivePlay[4]==23)
		 {
	     for(int currentStrategy=6;currentStrategy<16;currentStrategy++)
			{
			if(defensivePlay[currentStrategy]==19)
			  {
			  //OLLB	
			  blitzers[0]=true;
			  }
			if(defensivePlay[currentStrategy]==20)
			  {
			  //ILLB	
			  blitzers[1]=true;	
			  }			    
			if(defensivePlay[currentStrategy]==21)
			  {
			  //MLB	
			  blitzers[2]=true;
			  }
			if(defensivePlay[currentStrategy]==22)
			  {
			  //IRLB	
			  blitzers[3]=true;
			  }
			if(defensivePlay[currentStrategy]==23)
			  {
			  //ORLB	
			  blitzers[4]=true;
			  }
			if(defensivePlay[currentStrategy]==24)
			  {
			  //LCB	
			  blitzers[5]=true;
			  }			    
			if(defensivePlay[currentStrategy]==25)
			  {
			  //Nickel Back	
			  blitzers[6]=true;
			  }
			if(defensivePlay[currentStrategy]==26)
			  {
			  //FS	
			  blitzers[7]=true;
			  }				
			if(defensivePlay[currentStrategy]==27)
			  {
			  //SS	
			  blitzers[8]=true;
			  }
			if(defensivePlay[currentStrategy]==28)
			  {
			  //RCB	
			  blitzers[9]=true;
			  }			    
			if(defensivePlay[currentStrategy]==67)
			  {
			  //Dime Back	
			  blitzers[10]=true;
			  }
		    }   
		 }
		//Create new coverGuys table containing only those defenders in coverage currently on the pitch.
		int currentCoverGuy=0;
		for(currentCoverGuy=0;currentCoverGuy<coverGuys.length;currentCoverGuy++)
		   {
		   coverGuys[currentCoverGuy][0]=-1;	
		   }
		currentCoverGuy=0;   
		//LCB	
		if(defensiveSelectedPlayers[15]!=-1)
		  {	
		  coverGuys[currentCoverGuy][0]=defensiveSelectedPlayers[15];	
		  coverGuys[currentCoverGuy][1]=3;
		  coverGuys[currentCoverGuy][2]=21;
		  currentCoverGuy++;
		  }
		//RCB	
		if(defensiveSelectedPlayers[20]!=-1)
		  {	
		  coverGuys[currentCoverGuy][0]=defensiveSelectedPlayers[20];	
		  coverGuys[currentCoverGuy][1]=3;
		  coverGuys[currentCoverGuy][2]=21;
		  currentCoverGuy++;
		  }
		//FS	
		if(defensiveSelectedPlayers[18]!=-1)
		  {	
		  coverGuys[currentCoverGuy][0]=defensiveSelectedPlayers[18];	
		  coverGuys[currentCoverGuy][1]=3;
		  coverGuys[currentCoverGuy][2]=26;
		  currentCoverGuy++;
		  }			 			 
		//SS	
		if(defensiveSelectedPlayers[19]!=-1)
		  {	
		  coverGuys[currentCoverGuy][0]=defensiveSelectedPlayers[19];	
		  coverGuys[currentCoverGuy][1]=3;
		  coverGuys[currentCoverGuy][2]=27;
		  currentCoverGuy++;
		  }	
		//Nickel Back	
		if(defensiveSelectedPlayers[16]!=-1)
		  {	
		  coverGuys[currentCoverGuy][0]=defensiveSelectedPlayers[16];	
		  coverGuys[currentCoverGuy][1]=3;
		  coverGuys[currentCoverGuy][2]=21;
		  currentCoverGuy++;
		  }	
		//Dime Back	
		if(defensiveSelectedPlayers[17]!=-1)
		  {	
		  coverGuys[currentCoverGuy][0]=defensiveSelectedPlayers[17];	
		  coverGuys[currentCoverGuy][1]=3;
		  coverGuys[currentCoverGuy][2]=21;
		  currentCoverGuy++;
		  }		  	  
		//OLLB	
		if(defensiveSelectedPlayers[10]!=-1&&!blitzers[0])
		  {	
		  coverGuys[currentCoverGuy][0]=defensiveSelectedPlayers[10];	
		  coverGuys[currentCoverGuy][1]=2;
		  coverGuys[currentCoverGuy][2]=20;
		  currentCoverGuy++;
		  }	
		//ORLB	
		if(defensiveSelectedPlayers[14]!=-1&&!blitzers[4])
		  {	
		  coverGuys[currentCoverGuy][0]=defensiveSelectedPlayers[14];
		  coverGuys[currentCoverGuy][1]=2;	
		  coverGuys[currentCoverGuy][2]=20;
		  currentCoverGuy++;
		  }	
		//ILLB	
		if(defensiveSelectedPlayers[11]!=-1&&!blitzers[1])
		  {	
		  coverGuys[currentCoverGuy][0]=defensiveSelectedPlayers[11];
		  coverGuys[currentCoverGuy][1]=2;	
		  coverGuys[currentCoverGuy][2]=19;
		  currentCoverGuy++;
		  }
		//IRLB	
		if(defensiveSelectedPlayers[13]!=-1&&!blitzers[3])
		  {	
		  coverGuys[currentCoverGuy][0]=defensiveSelectedPlayers[13];	
		  coverGuys[currentCoverGuy][1]=2;
		  coverGuys[currentCoverGuy][2]=19;
		  currentCoverGuy++;
		  }		  
		//MLB	
		if(defensiveSelectedPlayers[12]!=-1&&!blitzers[2])
		  {	
		  coverGuys[currentCoverGuy][0]=defensiveSelectedPlayers[12];
		  coverGuys[currentCoverGuy][1]=2;	
		  coverGuys[currentCoverGuy][2]=19;
		  currentCoverGuy++;
		  }
		//Default defensive formation is for strong Right, switch if strong left found.
     	if(offensivePlay[4]!=29&&offensivePlay[4]!=30)
		  { 
		  if(offensiveSelectedPlayers[1]!=-1)
			{
			strongLeft=true;
		    flipFloppedSafeties=true;	
			playResult[57]=1;
			int tempPlayer=0;
			boolean tempBlitz=false;
			int tempPlayResult=0;
			tempPlayer=defensiveSelectedPlayers[18];
			tempBlitz=blitzers[7];
			tempPlayResult=playResult[51];
			defensiveSelectedPlayers[18]=defensiveSelectedPlayers[19];
			defensiveSelectedPlayers[19]=tempPlayer;
			blitzers[7]=blitzers[8];
			playResult[51]=playResult[52];
			blitzers[8]=tempBlitz;
			playResult[52]=tempPlayResult;
			}
		  if(offensiveSelectedPlayers[7]!=-1)
		    {	
		    strongRight=true;	
		    }
		  }	
		int coverageTableLine=0; 
		if((offensivePlay[4]>=1&&offensivePlay[4]<=8)||offensivePlay[4]==9||(offensivePlay[4]>=10&&offensivePlay[4]<=15&&offensivePlay[4]!=12)||offensivePlay[4]==39||offensivePlay[4]==12)
		  {  
		  for(int currentPlayer=0;currentPlayer<receivers.length;currentPlayer++)
			 {
			 if(receivers[currentPlayer]!=-1)
			   {
			   if(offensivePlayers[receivers[currentPlayer]][0]==keyRunner)
				 {
				 keyRunnerFound=true;
				 System.out.println("KeyRunner Found");		
				 }
			   if(offensivePlayers[receivers[currentPlayer]][0]==keyReceiver)
				 {
				 keyReceiverFound=true;
				 System.out.println("KeyReceiver Found");
				 keyReceiverNumber=coverageTableLine;		
				 }	
			   coverageTable[coverageTableLine][0]=offensivePlayers[receivers[currentPlayer]][1];	
			   coverageTable[coverageTableLine][1]=receivers[currentPlayer];
			   coverageTable[coverageTableLine][2]=-1;
			   coverageTable[coverageTableLine][3]=-1;
			   coverageTable[coverageTableLine][4]=-1;
			   coverageTableLine++;
		       }  	
			 }
		   //coverageTable WR assignment
		   for(int currentReceiver=0;currentReceiver<coverageTable.length;currentReceiver++)
		      {
		      if(coverageTable[currentReceiver][0]==13)
		        {	
		        for(int currentDefender=0;currentDefender<coverGuys.length;currentDefender++)
				   {
				   if(coverGuys[currentDefender][0]!=-1)
				     {
				     coverageTable[currentReceiver][2]=coverGuys[currentDefender][0];
				     coverGuys[currentDefender][0]=-1;	
				     currentDefender=coverGuys.length;
				     }
				   }
		        }
		      }  
		   //coverageTable TE assignment
		   for(int currentReceiver=0;currentReceiver<coverageTable.length;currentReceiver++)
			  {
			  boolean playerCovered=false;	
			  if(coverageTable[currentReceiver][0]==12)
				{	
				for(int currentDefender=0;currentDefender<coverGuys.length;currentDefender++)
				   {
				   if(coverGuys[currentCoverGuy][2]==27)
                      {
                      coverageTable[currentReceiver][2]=coverGuys[currentDefender][0];
					  coverGuys[currentDefender][0]=-1;	
					  currentDefender=coverGuys.length;	
					  playerCovered=true;
					  }
				   }
				}
			  if(coverageTable[currentReceiver][0]==12&&!playerCovered)
				{	
				for(int currentDefender=0;currentDefender<coverGuys.length;currentDefender++)
				   {
				   if(coverGuys[currentDefender][0]!=-1&&coverGuys[currentDefender][2]!=26)
					 {
					 boolean validPlayer=false;	
					 //Anyone other than a LB is able to cover anyone position.
					 if(coverGuys[currentDefender][2]!=19&&coverGuys[currentDefender][2]!=20)
					   {
					   validPlayer=true;	
					   }
					 //Only allows a LB lined up on the left to cover a TE on the left.
					 if((coverageTable[currentReceiver][1]==offensiveSelectedPlayers[0]||
					     coverageTable[currentReceiver][1]==offensiveSelectedPlayers[1])
					     &&(coverGuys[currentDefender][2]==19||coverGuys[currentDefender][2]==20)
					     &&(coverGuys[currentDefender][0]==defensiveSelectedPlayers[10]||
					        coverGuys[currentDefender][0]==defensiveSelectedPlayers[11]||
					        coverGuys[currentDefender][0]==defensiveSelectedPlayers[12]))
	                    {
	                    validPlayer=true;
                        covering[0]=true;
						}
                     //Only allows a LB lined up on the right to cover a TE on the right.   
					 if((coverageTable[currentReceiver][1]==offensiveSelectedPlayers[7]||
						 coverageTable[currentReceiver][1]==offensiveSelectedPlayers[8])
						 &&(coverGuys[currentDefender][2]==19||coverGuys[currentDefender][2]==20)
						 &&(coverGuys[currentDefender][0]==defensiveSelectedPlayers[12]||
							coverGuys[currentDefender][0]==defensiveSelectedPlayers[13]||
							coverGuys[currentDefender][0]==defensiveSelectedPlayers[14]))
						{
						validPlayer=true;
						covering[1]=true;	
						}
					 if(validPlayer)
					   {	
					   coverageTable[currentReceiver][2]=coverGuys[currentDefender][0];
					   coverGuys[currentDefender][0]=-1;	
					   currentDefender=coverGuys.length;
					   playerCovered=true;	
					   }
					 }
				   }
				}
			  if(coverageTable[currentReceiver][0]==12&&!playerCovered)
				{	
			    System.out.println("Using FS");		
				for(int currentDefender=0;currentDefender<coverGuys.length;currentDefender++)
				   {
				   if(coverGuys[currentCoverGuy][2]==26)
					 { 
					 coverageTable[currentReceiver][2]=coverGuys[currentDefender][0];
					 coverGuys[currentDefender][0]=-1;	
					 currentDefender=coverGuys.length;	
					 playerCovered=true;
					 }
				   }
				}				
			  }  
		   //coverageTable RB assignment
		   boolean RB1=false;
		   boolean RB2=false;
		   boolean RB3=false;
		   for(int currentReceiver=0;currentReceiver<coverageTable.length;currentReceiver++)
			  {
			  if(coverageTable[currentReceiver][0]==15||coverageTable[currentReceiver][0]==16)
				{	
				if(offensiveSelectedPlayers[12]!=-1&&!RB1)
				  {
				  //Find Someone to cover RB1	
				  int RBNumber=offensiveSelectedPlayers[12];	
				  if(defensiveSelectedPlayers[10]!=-1&&!blitzers[0]&&!covering[0])
				    {
				    //Use OLLB to cover RB1	
				    int LBNumber=defensiveSelectedPlayers[10];
					for(int currentRB=0;currentRB<coverageTable.length;currentRB++)
				       {
				       if(coverageTable[currentRB][1]==RBNumber)
				         {
						 for(int currentLB=0;currentLB<coverGuys.length;currentLB++)
				            {
				            if(coverGuys[currentLB][0]==LBNumber)
				              {
							  coverageTable[currentRB][2]=defensiveSelectedPlayers[10];
                              coverGuys[currentLB][0]=-1;
							  RB1=true;
							  }
				            }
				         }
				       }
				     }
				  if(defensiveSelectedPlayers[11]!=-1&&!blitzers[1]&&!RB1)
					{
					//Use ILLB to cover RB1	
					int LBNumber=defensiveSelectedPlayers[11];
					for(int currentRB=0;currentRB<coverageTable.length;currentRB++)
					   {
					   if(coverageTable[currentRB][1]==RBNumber)
						 {
						 for(int currentLB=0;currentLB<coverGuys.length;currentLB++)
							{
							if(coverGuys[currentLB][0]==LBNumber)
							  {
							  coverageTable[currentRB][2]=defensiveSelectedPlayers[11];
							  coverGuys[currentLB][0]=-1;
							  RB1=true;
							  }
							}
						  }
						}
					 }				     
				  if(defensiveSelectedPlayers[12]!=-1&&!blitzers[2]&&!RB1)
					{
					//Use MLB to cover RB1	
					int LBNumber=defensiveSelectedPlayers[12];
					for(int currentRB=0;currentRB<coverageTable.length;currentRB++)
					   {
					   if(coverageTable[currentRB][1]==RBNumber)
						 {
						 for(int currentLB=0;currentLB<coverGuys.length;currentLB++)
							{
							if(coverGuys[currentLB][0]==LBNumber)
							  {
							  System.out.println("RB1 covered by MLB");			
							  coverageTable[currentRB][2]=defensiveSelectedPlayers[12];
							  coverGuys[currentLB][0]=-1;
							  RB1=true;
							  }
							}
						  }
					    }
					 }				
				   }
				if(offensiveSelectedPlayers[12]!=-1&&!RB1&&defensivePlay[2]!=16&&
				   !(defensivePlay[2]==31&&blitzers[0]&&blitzers[2])&&
				   !(offensivePlay[2]==8&&defensivePlay[2]==26&&blitzers[2]))
				  {
				  System.out.println("Fixture="+fixtureNumber+" RB1 not covered, offplay= "+offensivePlay[2]+", defplay="+defensivePlay[2]+",OLLB-Blitz="+blitzers[0]+"covering="+covering[0]+",ILLB-Blitz="+blitzers[1]+",MLB-Blitz="+blitzers[2]);	   
				  }
				if(offensiveSelectedPlayers[13]!=-1&&!RB2)
				  {
				  //Find Someone to cover RB2	
				  int RBNUmber=offensiveSelectedPlayers[13];	
				  if(defensiveSelectedPlayers[14]!=-1&&!blitzers[4]&&!covering[1])
				    {
					//Use ORLB to cover RB2	
					int LBNumber=defensiveSelectedPlayers[14];
					for(int currentRB=0;currentRB<coverageTable.length;currentRB++)
					   {
					   if(coverageTable[currentRB][1]==RBNUmber)
						 {
						 for(int currentLB=0;currentLB<coverGuys.length;currentLB++)
							{
							if(coverGuys[currentLB][0]==LBNumber)
							  {
							  coverageTable[currentRB][2]=defensiveSelectedPlayers[14];
							  coverGuys[currentLB][0]=-1;
							  RB2=true;
							  }
							}
						  }
						}
					 }
				  if(defensiveSelectedPlayers[13]!=-1&&!blitzers[3]&&!RB2)
					{
					//Use IRLB to cover RB2	
					int LBNumber=defensiveSelectedPlayers[13];
					for(int currentRB=0;currentRB<coverageTable.length;currentRB++)
					   {
					   if(coverageTable[currentRB][1]==RBNUmber)
						 {
						 for(int currentLB=0;currentLB<coverGuys.length;currentLB++)
						    {
							if(coverGuys[currentLB][0]==LBNumber)
							  {
							  coverageTable[currentRB][2]=defensiveSelectedPlayers[13];
							  coverGuys[currentLB][0]=-1;
							  RB2=true;
							  }
							}
						  }
					    }
					  }				     
				  if(defensiveSelectedPlayers[12]!=-1&&!blitzers[2]&&!RB2)
					{
					//Use MLB to cover RB2	
					int LBNumber=defensiveSelectedPlayers[12];
					for(int currentRB=0;currentRB<coverageTable.length;currentRB++)
					   {
					   if(coverageTable[currentRB][1]==RBNUmber)
						 {
						 for(int currentLB=0;currentLB<coverGuys.length;currentLB++)
							{
							if(coverGuys[currentLB][0]==LBNumber)
							  {
							  System.out.println("RB2 covered by MLB");			
							  coverageTable[currentRB][2]=defensiveSelectedPlayers[12];
							  coverGuys[currentLB][0]=-1;
							  RB2=true;
							  }
							}
						  }
						}
					  }				
				   }
			    if(offensiveSelectedPlayers[13]!=-1&&!RB2&&offensivePlay[2]!=12&&offensivePlay[2]!=13)
				  {
				  System.out.println("RB2 not covered, offensiveplay="+offensivePlay[2]+" defensiveplay="+defensivePlay[2]+",ORLB-Blitz="+blitzers[4]+"covering="+covering[1]+",IRLB-Blitz="+blitzers[3]+",MLB-Blitz="+blitzers[2]);	   
				  }
		        if(offensiveSelectedPlayers[14]!=-1&&!RB3)
				  {
				  //Find Someone to cover RB3	
				  int RBNumber=offensiveSelectedPlayers[14];	
				  if(defensiveSelectedPlayers[11]!=-1&&!blitzers[1]&&!RB3)
					{
					//Use ILLB to cover RB3	
					int LBNumber=defensiveSelectedPlayers[11];
					for(int currentRB=0;currentRB<coverageTable.length;currentRB++)
					   {
					   if(coverageTable[currentRB][1]==RBNumber)
						 {
						 for(int currentLB=0;currentLB<coverGuys.length;currentLB++)
						    {
							if(coverGuys[currentLB][0]==LBNumber)
							  {
							  coverageTable[currentRB][2]=defensiveSelectedPlayers[11];
							  coverGuys[currentLB][0]=-1;
							  RB3=true;
							  }
						    }
						  }
						}
					}
				  if(defensiveSelectedPlayers[13]!=-1&&!blitzers[3]&&!RB3)
					{
					//Use IRLB to cover RB3	
					int LBNumber=defensiveSelectedPlayers[13];
					for(int currentRB=0;currentRB<coverageTable.length;currentRB++)
					   {
					   if(coverageTable[currentRB][1]==RBNumber)
						 {
						 for(int currentLB=0;currentLB<coverGuys.length;currentLB++)
						    {
							if(coverGuys[currentLB][0]==LBNumber)
							  {
							  coverageTable[currentRB][2]=defensiveSelectedPlayers[13];
							  coverGuys[currentLB][0]=-1;
							  RB3=true;
							  }
							}
						  }
						}
					}				     
				  if(defensiveSelectedPlayers[12]!=-1&&!blitzers[2]&&!RB3)
				    {
					//Use MLB to cover RB3	
					int LBNumber=defensiveSelectedPlayers[12];
					for(int currentRB=0;currentRB<coverageTable.length;currentRB++)
					   {
					   if(coverageTable[currentRB][1]==RBNumber)
						 {
						 for(int currentLB=0;currentLB<coverGuys.length;currentLB++)
						    {
							if(coverGuys[currentLB][0]==LBNumber)
							  {
							  coverageTable[currentRB][2]=defensiveSelectedPlayers[12];
							  coverGuys[currentLB][0]=-1;
							  RB3=true;
							  }
							}
						  }
						}
				      }	
				    }
				 if(offensiveSelectedPlayers[14]!=-1&&offensivePlayers[offensiveSelectedPlayers[14]][1]!=14&&!RB3&&defensivePlay[2]!=16&&!(offensivePlay[2]==5&&defensivePlay[2]==26&&blitzers[2])&&!(blitzers[0]&&blitzers[4]))
				   {
				   System.out.println("RB3 not covered, offensiveplay="+offensivePlay[2]+" defensiveplay="+defensivePlay[2]+",OLLB-Blitz="+blitzers[0]+"covering="+covering[0]+",ILLB-Blitz="+blitzers[1]+",MLB-Blitz="+blitzers[2]+",IRLB-Blitz="+blitzers[3]+",ORLB-Blitz="+blitzers[4]+"covering="+covering[1]);	   
				   }	
				 }
		      }//FOR ALL RB 
		   //doubleTeam assignment 
	       for(int currentPlayer=15;currentPlayer<defensiveSelectedPlayers.length;currentPlayer++)
		      {
			  if(defensiveSelectedPlayers[currentPlayer]!=-1)
		        {	
				for(int currentCover=0;currentCover<coverGuys.length;currentCover++)
				   {
				   if(defensiveSelectedPlayers[currentPlayer]==coverGuys[currentCover][0])
					 {
					 numOfFreeDB++;
					 freeDB[numOfFreeDB]=currentCover;			
					 }
				   }
		        }		
		      }
	       if(numOfFreeDB==-1)
		     {
		     if(!(offensivePlay[2]==8&&defensivePlay[2]==26&&blitzers[8])&&
		        !(defensivePlay[2]==16)&&
				!(offensivePlay[2]==7&&defensivePlay[2]==26&&blitzers[7])&&
				!(offensivePlay[2]==5&&defensivePlay[2]==26&&blitzers[7])&&
				!(offensivePlay[2]==5&&defensivePlay[2]==21)&&
				!(offensivePlay[2]==4&&defensivePlay[2]==21)&&
				!(offensivePlay[2]==5&&defensivePlay[2]==25)&&
				!(offensivePlay[2]==6&&defensivePlay[2]==21)&&
				!(offensivePlay[2]==7&&defensivePlay[2]==21))	
		        System.out.println("No ones homey="+"OffPlay="+offensivePlay[2]+" DefPlay="+defensivePlay[2]+"OLLB-Blitz"+blitzers[0]+",covering="+covering[0]+",ILLB-Blitz="+blitzers[1]+",MLB-Blitz="+blitzers[2]+",IRLB-Blitz="+blitzers[3]+"ORLB-Blitz"+blitzers[4]+",covering="+covering[1]+" LS[7]="+blitzers[7]+" RS[8]="+blitzers[8]);	   
		        }
			 if(numOfFreeDB>0)
			   {
			   if(numOfFreeDB==2)
				 {	
				 if(defensivePlay[5]==15)
					{	
					System.out.println("DoubleCover(Dime)");	
					}
				 else
				    {
				    if(defensivePlay[5]==16)
				      {	
				      }
				    else
				      {
				      if(defensivePlay[5]==17)
				        {	  
				        }  
				      else
				        {  
				        System.out.println("Dime");
				        }
				      }
				    }    
				 }	
			   if(numOfFreeDB>2)
				 {	
				 System.out.println("More than 2 free DB's for fixture("+fixtureNumber+")");
				 }				 		     	   
			   }		        
	       }//for all passing plays 
	   int runMod=0;
	   if(defensivePlay[4]==16)
	     {
	     keyFB1=true;	
	     defensivePlayFound=true;
	     }
	   if(defensivePlay[4]==40)
		 {
		 keyFB2=true;	
		 defensivePlayFound=true;
		 }	     
	   if(defensivePlay[4]==17)
		 {
		 keyRB1=true;
		 defensivePlayFound=true;
		 }
	   if(defensivePlay[4]==18)
		 {
		 keyRB2=true;	
		 defensivePlayFound=true;
		 }
	   if(defensivePlay[4]==19)
		 {
		 keyQB=true;	
		 defensivePlayFound=true;
		 }
	   if(defensivePlay[4]==20)
		 {
		 keyNone=true;	
		 defensivePlayFound=true;
		 }
	   if(defensivePlay[4]==21)
		 {
		 passDefense=true;	
		 defensivePlayFound=true;
		 }	
	   if(defensivePlay[4]==22)
		 {
		 preventDefense=true;	
		 defensivePlayFound=true;
		 }	
	   if(defensivePlay[4]==23)
		 {
		 blitz=true;	
		 defensivePlayFound=true;
		 }	
		if(offensivePlay[5]==3)
		  {
		  //RollOut
		  bootLeg=true;
		  }				 	 		 	 		 
		if(offensivePlay[5]==4)
		  {
		  //Draw	
		  if(defensivePlayNumber>15&&defensivePlayNumber<21)
			{
			runMod+=2;	
			}
		  if(defensivePlayNumber==21||defensivePlayNumber==22)
			{
			runMod-=2;	  
			}
		  if(defensivePlayNumber==23)
			{
			runMod-=4;	  
			}
		  }		 
	   if(numOfFreeDB>0&&keyReceiverFound)
	     {
		 System.out.println("Key Receiver Assignment");	
		 if(numOfFreeDB==1)
		   {
		   doubleTeamReceiver=true;
		   if(coverageTable[keyReceiverNumber][2]!=-1)
		     {
			 coverageTable[keyReceiverNumber][3]=coverGuys[freeDB[0]][0];	
		     }
		   else
		     {  
			 coverageTable[keyReceiverNumber][2]=coverGuys[freeDB[0]][0];	
		     }
		   coverGuys[freeDB[0]][0]=-1;
		   playResult[55]=offensivePlayers[coverageTable[0][1]][0];
		   System.out.println("DoubleCover of key receiver "+offensivePlayers[coverageTable[0][1]][0]+" by "+defensivePlayers[coverageTable[0][2]][0]+" and "+defensivePlayers[coverageTable[0][3]][0]);
		   }
		 if(numOfFreeDB==1)
		   {
		   tripleTeamReceiver=true;
		   if(coverageTable[keyReceiverNumber][2]!=-1)
			 {
			 coverageTable[keyReceiverNumber][3]=coverGuys[freeDB[0]][0];
			 coverageTable[keyReceiverNumber][4]=coverGuys[freeDB[1]][0];
			 }	
		   else
		     { 
			 coverageTable[keyReceiverNumber][2]=coverGuys[freeDB[0]][0];
			 coverageTable[keyReceiverNumber][3]=coverGuys[freeDB[1]][0];
			 }
		   coverGuys[freeDB[0]][0]=-1;
		   coverGuys[freeDB[1]][0]=-1;
		   playResult[55]=offensivePlayers[coverageTable[0][1]][0];
		   playResult[56]=offensivePlayers[coverageTable[0][1]][0];
		   System.out.println("TripleCover of key receiver "+offensivePlayers[coverageTable[0][1]][0]+" by "+defensivePlayers[coverageTable[0][2]][0]+" and "+defensivePlayers[coverageTable[0][3]][0]+" and "+defensivePlayers[coverageTable[0][4]][0]);
		   }	
	     }
	   if(!keyReceiverFound&&defensivePlay[5]==15&&numOfFreeDB>0)
		  {
		  doubleTeamReceiver=true;
		  coverageTable[0][3]=coverGuys[freeDB[0]][0];
		  coverGuys[freeDB[0]][0]=-1;
		  playResult[55]=offensivePlayers[coverageTable[0][1]][0];
		  }	
	   if(!keyReceiverFound&&defensivePlay[5]==16&&numOfFreeDB>1)
		  {
		  doubleTeamReceiverX2=true;
		  coverageTable[0][3]=coverGuys[freeDB[0]][0];
		  coverGuys[freeDB[0]][0]=-1;
		  coverageTable[1][3]=coverGuys[freeDB[1]][0];
		  coverGuys[freeDB[1]][0]=-1;
		  playResult[55]=offensivePlayers[coverageTable[0][1]][0];
		  playResult[56]=offensivePlayers[coverageTable[1][1]][0];
		  }	
	   if(!keyReceiverFound&&defensivePlay[5]==17&&numOfFreeDB>1)
		  {
		  tripleTeamReceiver=true;
		  coverageTable[0][3]=coverGuys[freeDB[0]][0];
		  coverGuys[freeDB[0]][0]=-1;
		  coverageTable[0][4]=coverGuys[freeDB[1]][0];
		  coverGuys[freeDB[1]][0]=-1;
		  playResult[55]=offensivePlayers[coverageTable[0][1]][0];
		  playResult[56]=offensivePlayers[coverageTable[0][1]][0];
		  }	
	   //Coverage Table finalised, now take a copy of the DB lineup prior to rejig.
	   if((offensivePlay[4]>=1&&offensivePlay[4]<=8)||offensivePlay[4]==9||(offensivePlay[4]>=10&&offensivePlay[4]<=15&&offensivePlay[4]!=12)||offensivePlay[4]==39||offensivePlay[4]==12)
	     {
	     int[] dbLineUp=new int[defensiveSelectedPlayers.length];
	     int currentDB=0;
	     for(int currentPlayer=0;currentPlayer<defensiveSelectedPlayers.length;currentPlayer++)
	        {	
	        dbLineUp[currentDB]=defensiveSelectedPlayers[currentPlayer];
	        currentDB++;	  
	        }
	     //Coverage Table finalised, now clear out defensive backfield.
	     for(int currentPlayer=15;currentPlayer<defensiveSelectedPlayers.length;currentPlayer++)
	        {
	        defensiveSelectedPlayers[currentPlayer]=-1;	   
	        }
         //Coverage Table finalised, now rejig defensive lineup to match.
         for(currentDB=15;currentDB<dbLineUp.length;currentDB++)
            {
            //Got a DB, now find him in coverage table.	
            for(int currentReceiver=0;currentReceiver<coverageTable.length;currentReceiver++)
               {
		       boolean found=false;
		       if(coverageTable[currentReceiver][0]!=0&&
		          (coverageTable[currentReceiver][2]==dbLineUp[currentDB]||
				   coverageTable[currentReceiver][3]==dbLineUp[currentDB]||
				   coverageTable[currentReceiver][4]==dbLineUp[currentDB]))
		  	     {
		  	     //Found DB within coverage table.
		  	     boolean primaryCover=false;
		  	     boolean secondaryCover=false;
		  	     boolean thirdCover=false;
		  	     if(coverageTable[currentReceiver][2]==dbLineUp[currentDB])
		  	       {
		  	       primaryCover=true;
		  	       }
			     if(coverageTable[currentReceiver][3]==dbLineUp[currentDB])
				   {
				   secondaryCover=true;
				   }
			     if(coverageTable[currentReceiver][4]==dbLineUp[currentDB])
				   {
				   thirdCover=true;
				   }
			     if(primaryCover)
			       {	 		  	     
			       for(int currentPosition=0;currentPosition<offensiveSelectedPlayers.length&&!found;currentPosition++)
                      {
                      if(coverageTable[currentReceiver][1]==offensiveSelectedPlayers[currentPosition])
                        {
                        found=true;
                        if(currentPosition==0||currentPosition==1)
                          {
                          //WR or TE Left	
						  if(defensiveSelectedPlayers[15]==-1)
						    {	     
						    defensiveSelectedPlayers[15]=dbLineUp[currentDB];	
						    }
						  else
						    {
						    System.out.println("K Spot already filled");	  
						    }
                          }
				        if(currentPosition==7||currentPosition==8)
				          {
                          //WR or TE Right
						  if(defensiveSelectedPlayers[20]==-1)
						    {	     
						    defensiveSelectedPlayers[20]=dbLineUp[currentDB];	
						    }
						  else
						    {
						    System.out.println("O Spot already filled");	  
						    }
						  }
				        if(currentPosition==10)
				          {
				          //FL Left	
						  if(defensiveSelectedPlayers[18]==-1)
						    {	     
						    defensiveSelectedPlayers[18]=dbLineUp[currentDB];	
						    }
						  else
						    {
						    System.out.println("M Spot already filled");	  
						    }
						  }
				        if(currentPosition==11)
				          {
				          //FL Right	
						  if(defensiveSelectedPlayers[19]==-1)
						    {	     
						    defensiveSelectedPlayers[19]=dbLineUp[currentDB];	
						    }
						  else
						    {
						    System.out.println("N Spot already filled");	  
						    }
				          }
                        }   
                      }
                    }
                 if(secondaryCover)
                   { 
				   if(defensiveSelectedPlayers[16]==-1)
                     {	     
				     defensiveSelectedPlayers[16]=dbLineUp[currentDB];	
                     }
                   else
                     {
					 if(defensiveSelectedPlayers[17]==-1)
					   {	     
					   defensiveSelectedPlayers[17]=dbLineUp[currentDB];	
					   }
					 else
					   {
					   System.out.println("Nickel Spot already filled");	  
					   }
                     }
                   }
                 if(thirdCover)
                   {  
				   if(defensiveSelectedPlayers[17]==-1)
				     {	     
				     defensiveSelectedPlayers[17]=dbLineUp[currentDB];	
				     }
				   else
				     {
				     System.out.println("Dime Spot already filled");	  
				     }
                   }
                 }
              }  
		  	}
			//Coverage Table finalised and rejigged, add back in DB's not covering a specific player.
			for(currentDB=0;currentDB<coverGuys.length;currentDB++)
			   {
			   if(coverGuys[currentDB][0]!=-1)
			     {	
			     for(int currentDB2=15;currentDB2<dbLineUp.length;currentDB2++)
			        {
			        if(coverGuys[currentDB][0]==dbLineUp[currentDB2])
			          {
			          if(defensiveSelectedPlayers[19]==-1)
                        {
                        //Put spare DB into FS spot.
						defensiveSelectedPlayers[19]=dbLineUp[currentDB2];	
                        }
                      else
                        {
						if(defensiveSelectedPlayers[18]==-1)
						  {
						  //Put spare DB into SS spot.	
						  defensiveSelectedPlayers[18]=dbLineUp[currentDB2];	
						  }
						else
						  {  
						  if(defensiveSelectedPlayers[16]==-1)
						    {
							//Put spare DB into Nickel spot.
							defensiveSelectedPlayers[16]=dbLineUp[currentDB2];	
							}
						  else
							{
							if(defensiveSelectedPlayers[17]==-1)
							  {
							  //Put spare DB into Dime spot.
							  defensiveSelectedPlayers[17]=dbLineUp[currentDB2];	
							  }
							else
							  {
							  System.out.println("No room for spare DB");	  
							  }
							}  
                          }
			            }
			          }
			        }  
			      }
			   }
		  int playerCount=0;
		  for(int currentPlayer=0;currentPlayer<defensiveSelectedPlayers.length;currentPlayer++)
		     {
		     if(defensiveSelectedPlayers[currentPlayer]!=-1)
		       {
		       playerCount++;			   
		       }
		     }
		  if(playerCount!=11)
		    {
		    System.out.println("");	
		    System.out.println("CoverageTable merge error");
			System.out.println("");
		    for(int currentPlayer=0;currentPlayer<dbLineUp.length;currentPlayer++)
		       {   
			   System.out.println("dbLineUp["+currentPlayer+"]="+dbLineUp[currentPlayer]);	
		       }
			System.out.println("");
			for(int currentPlayer=0;currentPlayer<defensiveSelectedPlayers.length;currentPlayer++)
			   {   
			   System.out.println("defensiveSelectedPlayer["+currentPlayer+"]="+defensiveSelectedPlayers[currentPlayer]);	
			   }		       
			System.out.println("");
			for(int currentPlayer=0;currentPlayer<coverGuys.length;currentPlayer++)
			   {   
			   System.out.println("coverGuys["+currentPlayer+"]="+coverGuys[currentPlayer][0]);	
			   }
		    }
          }
	   for(int currentStrategy=6;currentStrategy<16;currentStrategy++)
		  {
		  if(offensivePlay[currentStrategy]==6)
		    {
		    doubleTeamDefender=true;			
		    doubleTeamPosition=whoToDoubleTeam(defensiveTeam,offensivePlay[4],defensiveSelectedPlayers,defensivePlayers,database,run,random);
			playResult[17]=doubleTeamPosition;
			primaryDoubleTeamer=getPrimaryDoubleTeamer(doubleTeamPosition);
			playResult[18]=primaryDoubleTeamer;
            secondaryDoubleTeamer=getSecondaryDoubleTeamer(primaryDoubleTeamer,offensiveSelectedPlayers,offensivePlayers,random);
			playResult[19]=secondaryDoubleTeamer;
		    }
		 if(defensivePlay[currentStrategy]==18)
		   {
		   stunt1=true;	   
		   }
		 if(defensivePlay[currentStrategy]==66)
		   {
		   stunt1=false;
		   stunt2=true;	  
		   }
	     }
		int numOfDL=0; 
		if(stunt1||stunt2)
		  { 
		  for(int currentPlayer=0;currentPlayer<10;currentPlayer+=2)
			 {
			 if(defensiveSelectedPlayers[currentPlayer]!=-1)
			   {
			   numOfDL++;			 
			   }
			 }	 
          if(stunt1)
            {
            if(strongLeft&&!strongRight)
              {
              stuntRight=true;
              }
            if(!strongLeft&&strongRight)
              {
              stuntLeft=true;	  
              }
            if(strongLeft&&strongRight)
              {
              stuntRight=true;	
              }
            }
          if(stunt2)
            {
            stuntLeft=true;
            stuntRight=true;
            }
          }
	   if(offensivePlay[4]>=1&&offensivePlay[4]<=8)
	     {
	     playResult[0]=offensivePlayers[offensiveSelectedPlayers[positionNumber-1]][0];	
	     int ballCarrier=positionNumber;
		 int offensiveBlocking=0;
		 int defensiveTackling=0;	     	
         //Run	
	     int block1=0;
		 int block2=0;	  
		 int blockResult=0;
		 boolean inside=false;
		 boolean left=false;
		 boolean option=false;
		 int optionTo=0;
		 int numOfFB=0;
		 int numOfRB=0;
		 boolean optionToKeyed=false;
		 boolean runnerKeyed=false;
		 boolean rightCall=false;
		 boolean optionPitched=false;
		 boolean runAtStunt=false;
	     if(offensivePlay[4]==1)
	       {
	       //Inside Left	
	       offensivePlayFound=true;
	       inside=true;
	       left=true;
	       block1=23;
	       }
		 if(offensivePlay[4]==2)
		   {
		   //Inside Right	
		   offensivePlayFound=true;
		   inside=true;
		   block1=61;
		   }
		 if(offensivePlay[4]==3)
		   {
		   //Sweep Left	
		   offensivePlayFound=true;
		   left=true;
		   block1=4;
		   }
		 if(offensivePlay[4]==4)
		   {
		   //Sweep Right	
		   offensivePlayFound=true;
		   block1=42;
		   }
		 if(offensivePlay[4]==5)
		   {
		   //Pitch Left	
		   offensivePlayFound=true;
		   left=true;
		   block1=4;
		   block2=23;
		   }
		 if(offensivePlay[4]==6)
		   {
		   //Pitch Right	
		   offensivePlayFound=true;
		   block1=42;
		   block2=61;
		   }
		 if(offensivePlay[4]==7)
		   {
		   //Option Left	
		   offensivePlayFound=true;
		   inside=true;
		   left=true;
		   option=true;
		   block1=23;
		   block2=4;
		   }
		 if(offensivePlay[4]==8)
		   {
		   //Option Right
		   offensivePlayFound=true;	
		   inside=true;
		   option=true;
		   block1=61;
		   block2=42;
		   }
		 for(int currentRB=14;currentRB>11;currentRB--)
			{	
			if(formations[offensiveFormation][currentRB]==16)
			  {
			  numOfFB++;	
			  }
			if(formations[offensiveFormation][currentRB]==15)
			  {
			  numOfRB++;	
			  }			  
			} 		   		  
		 if(left&&option)
		   {   
		   for(int currentRB=14;currentRB>11;currentRB--)
		      {	
		      if(formations[offensiveFormation][currentRB]!=-1)
		        {
		        optionTo=currentRB;
		        currentRB=0;	
		        }
		      } 
		   }
		 if(!left&&option)
		   { 
		   if(formations[offensiveFormation][14]!=-1)
			 {
			 optionTo=14;
			 }
		   else
			 { 
			 if(formations[offensiveFormation][12]!=-1)
			   {
			   optionTo=12;
			   }
			 else
			   { 
			   if(formations[offensiveFormation][13]!=-1)
				 {
				 optionTo=13;
				 }
			   else
			     {	
				 Routines.writeToLog(servletName,"No-one to option to : " + fixtureNumber,false,context);	  			   	 
			     }
			   }
			 }
		   }
           if((optionTo==14||ballCarrier==15)&&
			 ((keyRB2&&formations[offensiveFormation][14]==15)||
			  (keyRB1&&formations[offensiveFormation][14]==15&&numOfRB==1)||
			  (keyFB2&&formations[offensiveFormation][14]==16)||
			  (keyFB1&&formations[offensiveFormation][14]==16&&numOfFB==1)))
			  {
			  if(optionTo==14)
			    {	
			    optionToKeyed=true;
			    }
			  else
			    {
			    runnerKeyed=true;	  	
			    }
			  }
		   if((optionTo==13||ballCarrier==14)&&
			 ((keyRB2&&formations[offensiveFormation][13]==15&&numOfRB==1)||
			  (keyRB2&&formations[offensiveFormation][13]==15&&numOfRB==2&&formations[offensiveFormation][12]==15)||
			  (keyRB1&&formations[offensiveFormation][13]==15&&numOfRB==1)||
			  (keyRB1&&formations[offensiveFormation][13]==15&&numOfRB==2&&formations[offensiveFormation][12]!=15)||
			  (keyFB2&&formations[offensiveFormation][13]==16&&numOfFB==1)||
			  (keyFB2&&formations[offensiveFormation][13]==16&&numOfFB==2&&formations[offensiveFormation][12]==16)||
			  (keyFB1&&formations[offensiveFormation][13]==16&&numOfFB==1)||
			  (keyFB1&&formations[offensiveFormation][13]==16&&numOfFB==2&&formations[offensiveFormation][12]!=16)))
			  {
			  if(optionTo==13)
				{	
				optionToKeyed=true;
				}
			  else
				{
				runnerKeyed=true;	  	
				}	
			  }
		   if((optionTo==12||ballCarrier==13)&&
			 ((keyRB2&&formations[offensiveFormation][12]==15&&numOfRB==1)||
			  (keyRB1&&formations[offensiveFormation][12]==15)||
			  (keyFB2&&formations[offensiveFormation][12]==16&&numOfFB==1)||
			  (keyFB1&&formations[offensiveFormation][12]==16)))
			  {
			  if(optionTo==12)
				{	
				optionToKeyed=true;
				}
			  else
				{
				runnerKeyed=true;	  	
				}	
			  }
		 int numOfChoices=1;  
		 if(block2>0)
		   {
		   numOfChoices=2;	  
		   }
		 for(int currentChoice=0;currentChoice<numOfChoices;currentChoice++)
		   {  
		   currentMatchup++;	
		   currentTackler2=0;
		   currentBlocker=0;
		   boolean offensiveBlock=false;
		   boolean defensiveTackle=false;
		   boolean emptyBox=true;
		   int numOfExceptionalBlockers=0;
		   int tempBlock=0;
		   int tempBlockResult=0;
		   int tempBlockingWinner=0;
		   if(currentChoice==0)
		     {
		     tempBlock=block1;	
		     }
		   else
		     {
		     tempBlock=block2;	  
		     }
           for(int currentBlock=0;currentBlock<19;currentBlock++)
		      {
		      if(currentBlock<9)
		        {
		        //Offensive Blocking	
		        if(currentBlock==0&&shuffledActionCards[cardData[0]][tempBlock+currentBlock]==1)
		          {
		          //Breakout	
		          offensiveBlock=true;
		          offensiveBlocking=99;
		          currentBlock=20;			  
		          }
		        if(currentBlock==1&&shuffledActionCards[cardData[0]][tempBlock+currentBlock]==1)
		          {
		          //LE Blocking
				  offensiveBlock=true;
 	              int blockingSkill=0;	
 	              int blockingValue=0;
				  if(offensiveSelectedPlayers[0]!=-1)
				    {
		            blockingSkill=getBlockingSkill(offensivePlayers[offensiveSelectedPlayers[0]][1],true);
		            blockingValue=getSkill(false,offensiveSelectedPlayers[0],offensivePlayers,blockingSkill,1,true);
		            if(blockingValue==5)
		              {
					  numOfExceptionalBlockers++;	
		              }
	                offensiveBlocking+=blockingValue;
	                blockers[currentMatchup][currentBlocker]=currentBlock;
	                currentBlocker++;
				    }
                  if(offensiveSelectedPlayers[1]!=-1)
				    {  	
				    blockingSkill=getBlockingSkill(offensivePlayers[offensiveSelectedPlayers[1]][1],true);
				    blockingValue=getSkill(false,offensiveSelectedPlayers[1],offensivePlayers,blockingSkill,1,true);	  	            
				    if(blockingValue==5)
					  {
					  numOfExceptionalBlockers++;	
					  }				  
				    offensiveBlocking+=blockingValue;
					blockers[currentMatchup][currentBlocker]=currentBlock;
					currentBlocker++;	
			        }
			      if(doubleTeamDefender&&currentChoice==0)
			        {  
			        if(!primaryDoubleTeamerReferenced) 
			          { 
			          primaryDoubleTeamerReferenced=isDoubleTeamerReferenced(doubleTeamDefender,primaryDoubleTeamer,currentBlock);
			          }
			        if(!secondaryDoubleTeamerReferenced) 
			          { 	
			          secondaryDoubleTeamerReferenced=isDoubleTeamerReferenced(doubleTeamDefender,secondaryDoubleTeamer,currentBlock);
			          }	
			        }
		          }
			    if(currentBlock>=2&&currentBlock<=6&&shuffledActionCards[cardData[0]][tempBlock+currentBlock]==1)
				  {
				  //OL Blocking	
				  offensiveBlock=true;
				  int blockingSkill=0;	
				  int blockingValue=0;
				  blockingSkill=getBlockingSkill(offensivePlayers[offensiveSelectedPlayers[currentBlock]][1],true);
				  blockingValue=getSkill(false,offensiveSelectedPlayers[currentBlock],offensivePlayers,blockingSkill,1,true);
				  if(blockingValue==5)
				    {
				    numOfExceptionalBlockers++;	
				    }				
				  offensiveBlocking+=blockingValue;	 
				  if(doubleTeamDefender&&currentChoice==0)
				    { 
					if(!primaryDoubleTeamerReferenced) 
					  { 
					  primaryDoubleTeamerReferenced=isDoubleTeamerReferenced(doubleTeamDefender,primaryDoubleTeamer,currentBlock);
					  }
					if(!secondaryDoubleTeamerReferenced) 
					  { 	
					  secondaryDoubleTeamerReferenced=isDoubleTeamerReferenced(doubleTeamDefender,secondaryDoubleTeamer,currentBlock);
					  }
					}
				  blockers[currentMatchup][currentBlocker]=currentBlock;
				  currentBlocker++;
				  }		        
			    if(currentBlock==7&&shuffledActionCards[cardData[0]][tempBlock+currentBlock]==1)
				  {
				  //RE Blocking	
				  offensiveBlock=true;
				  int blockingSkill=0;	
				  int blockingValue=0;
				  if(offensiveSelectedPlayers[7]!=-1)
				    {
				    blockingSkill=getBlockingSkill(offensivePlayers[offensiveSelectedPlayers[7]][1],true);
				    blockingValue=getSkill(false,offensiveSelectedPlayers[7],offensivePlayers,blockingSkill,1,true);
				    if(blockingValue==5)
					  {
					  numOfExceptionalBlockers++;	
					  }				  
				    offensiveBlocking+=blockingValue;	 
					blockers[currentMatchup][currentBlocker]=currentBlock;
					currentBlocker++;
				    }
 				  if(offensiveSelectedPlayers[8]!=-1)
				    {   
				    blockingSkill=getBlockingSkill(offensivePlayers[offensiveSelectedPlayers[8]][1],true);
				    blockingValue=getSkill(false,offensiveSelectedPlayers[8],offensivePlayers,blockingSkill,1,true);
				    if(blockingValue==5)
					  {
					  numOfExceptionalBlockers++;	
					  }				  
				    offensiveBlocking+=blockingValue;	
					blockers[currentMatchup][currentBlocker]=currentBlock;
					currentBlocker++;  	
				    }
				 if(doubleTeamDefender&&currentChoice==0)
				   {   
				   if(!primaryDoubleTeamerReferenced) 
				     { 
				     primaryDoubleTeamerReferenced=isDoubleTeamerReferenced(doubleTeamDefender,primaryDoubleTeamer,currentBlock);
				     }
				   if(!secondaryDoubleTeamerReferenced) 
				     { 	
				     secondaryDoubleTeamerReferenced=isDoubleTeamerReferenced(doubleTeamDefender,secondaryDoubleTeamer,currentBlock);
				     }
				   }
			     }		        
			    if(currentBlock==8&&shuffledActionCards[cardData[0]][tempBlock+currentBlock]==1)
				  {
				  //BK Blocking	
				  offensiveBlock=true;
				  int blockingSkill=0;
				  int blockingValue=0;
				  if(offensivePlay[3]!=13)
				    {
				    if(offensiveSelectedPlayers[12]!=-1)
				      {	
				      blockingSkill=getBlockingSkill(offensivePlayers[offensiveSelectedPlayers[12]][1],true);
				      blockingValue=getSkill(false,offensiveSelectedPlayers[12],offensivePlayers,blockingSkill,1,true);	
					  if(blockingValue==5)
					    {
					    numOfExceptionalBlockers++;	
					    }				    
				      offensiveBlocking+=blockingValue;
					  blockers[currentMatchup][currentBlocker]=currentBlock;
					  currentBlocker++;
				      }
				    }
				  if(offensivePlay[3]!=14)
				    {
				    if(offensiveSelectedPlayers[13]!=-1)
					  {	
				      blockingSkill=getBlockingSkill(offensivePlayers[offensiveSelectedPlayers[13]][1],true);
				      blockingValue=getSkill(false,offensiveSelectedPlayers[13],offensivePlayers,blockingSkill,1,true);
					  if(blockingValue==5)
					    {
					    numOfExceptionalBlockers++;	
					    }				    
				      offensiveBlocking+=blockingValue;	
					  blockers[currentMatchup][currentBlocker]=currentBlock;
					  currentBlocker++;
					  }
				    }
				  if(offensivePlay[3]!=15)
				    {
				    if(offensiveSelectedPlayers[14]!=-1&&offensiveSelectedPlayers[14]==14	)
				  	  {
				 	  blockingSkill=getBlockingSkill(offensivePlayers[offensiveSelectedPlayers[14]][1],true);
					  blockingValue=getSkill(false,offensiveSelectedPlayers[14],offensivePlayers,blockingSkill,1,true);	
					  if(blockingValue==5)
					    {
					    numOfExceptionalBlockers++;
					    }					
				      offensiveBlocking+=blockingValue;
					  blockers[currentMatchup][currentBlocker]=currentBlock;
					  currentBlocker++;
					  }
				    }
                  }//End Of Back Blocking  
		        }//End Of Offensive Blocking
		      else
		        {  
		        // Defensive Blocking
			    if(shuffledActionCards[cardData[0]][tempBlock+currentBlock]==1)
				  {
				  defensiveTackle=true;
				  boolean doubledUp=false;
				  int tackleSkill=0;	
				  int tackleValue=0;
				  int tackler=currentBlock-9;
				  int numOfTacklers=0;
				  if(tackler>4)
				    {
				    numOfTacklers=1;	
				    }
				  else
				    {
				    numOfTacklers=2;	  
				    }
				  switch(tackler)
				    {
				    case 0:
				     tackler=0;
				 	 break;
				    case 1:
				     tackler=2;
				     break;				  
				    case 2:
				     tackler=4;
				     break;
				    case 3:
				     tackler=6;
				     break;
				    case 4:
				     tackler=8;
				     break;
				    case 5:
				     tackler=10;
				     break;
				    case 6:
				     tackler=11;
				     break;
				    case 7:
				     tackler=12;
				     break;
				    case 8:
				     tackler=13;
				     break;
				    case 9:
				     tackler=14;
				     break;
				    default:
				  	Routines.writeToLog(servletName,"No Num Of Tacklers for : " + tackler,false,context);
				    }
				  int numOfExceptionalTacklers=0;
				  for(int currentTackler=tackler;currentTackler<(tackler+numOfTacklers);currentTackler++)
				     {  
				     if(defensiveSelectedPlayers[currentTackler]!=-1
				        &&!(currentTackler==10&&blitzers[0])
						&&!(currentTackler==10&&covering[0])
					    &&!(currentTackler==11&&blitzers[1])
					    &&!(currentTackler==12&&blitzers[2])
					    &&!(currentTackler==13&&blitzers[3])
					    &&!(currentTackler==14&&blitzers[4])
						&&!(currentTackler==14&&covering[1]))
				       {
				       emptyBox=false;	
				       if(numOfDL==3)
				         {
				         if(stuntLeft)
				           {
				           if(currentTackler==0||currentTackler==4)
				             {
				             runAtStunt=true; 	
				             }
				           }
				         if(stuntRight)
				           {  
							if(currentTackler==4||currentTackler==8)
							  {
							  System.out.println("Run at 3 Stunt Right");	
							  runAtStunt=true;	
							  }
				           }
				         }
					   if(numOfDL==4||numOfDL==5)
						 {
						 if(stuntLeft)
						   {
						   if(currentTackler==0||currentTackler==2)
							 {
							 if(numOfDL==5)
							   {
							   System.out.println("Run at "+numOfDL+" Stunt Left");
							   }	
							 runAtStunt=true; 	
							 }
						   }
						 if(stuntRight)
						   {  
						   if(currentTackler==6||currentTackler==8)
							 {
							 if(numOfDL==5)
							   {	
							   System.out.println("Run at "+numOfDL+" Stunt Right");
							   }	
							 runAtStunt=true;	
							 }
						   }
						 }
				       tackleSkill=getBlockingSkill(defensivePlayers[defensiveSelectedPlayers[currentTackler]][1],true);
				       tackleValue=getSkill(false,defensiveSelectedPlayers[currentTackler],defensivePlayers,tackleSkill,1,true);
					   if(tackleSkill==0)
						 {	
						 Routines.writeToLog(servletName,"["+fixtureNumber+"]Tackle Not Found",false,context);	
						 }
					   if(tackleValue==-5&&offensiveBlock)
				         {
				         tackleValue=-4;	
				         numOfExceptionalTacklers++;
				         }
				       defensiveTackling+=tackleValue;
				       if(currentTackler==(tackler+1))
				         {
				         doubledUp=true;
				         }
					   tacklers[currentMatchup][currentTackler2]=currentTackler;
					   currentTackler2++;  
				       }
				     }
				  if(doubledUp)
				    {
				    defensiveTackling+=numOfExceptionalTacklers;	  	  
				    if(defensiveTackling<4)
					  {
				 	  defensiveTackling=-4;		
					  }
				    if(defensiveTackling>5)
				      {
				      defensiveTackling=-5;		
				      }
				    }
				  }
		        }
 	          }//End Of Blocking Calculations
			if(doubleTeamDefender&&primaryDoubleTeamerReferenced)
			  {
			  offensiveBlocking=4;
			  if(numOfExceptionalBlockers>0)
				{
				offensiveBlocking=5;		  
				}
			  }
			if(doubleTeamDefender&&!primaryDoubleTeamerReferenced&&secondaryDoubleTeamerReferenced)
			  {
			  if(offensiveBlocking>0)
				{
				offensiveBlocking=0;		  
				}
			  }		      
			if(defensiveTackle&&!doubleTeamDefender)
			  {
			  offensiveBlocking-=numOfExceptionalBlockers;	  
			  }
			if(!offensiveBlock&&emptyBox)
			  { 
			  offensiveBlocking=2;	 
			  }
		    if(offensiveBlock&&!defensiveTackle)
			  {  
			  tempBlockResult=offensiveBlocking;
			  tempBlockingWinner=1;
			  }
			if(!offensiveBlock&&defensiveTackle)
			  {  
			  tempBlockResult=defensiveTackling;
			  tempBlockingWinner=-1;
			  }
			if(!offensiveBlock&&!defensiveTackle)
			  {  
			  System.out.println("!!!No Blocking Found:"+fixtureNumber);
			  }
			if(offensiveBlock&&defensiveTackle)
			  {  
			  if(offensiveBlocking+defensiveTackling>0)
			    {	
			    tempBlockResult=offensiveBlocking;
				tempBlockingWinner=1;
			    }
			  if(offensiveBlocking+defensiveTackling<0)
				{	
				tempBlockResult=defensiveTackling;
				tempBlockingWinner=-1;
				}
			  if(offensiveBlocking+defensiveTackling==0)
				{	
				tempBlockResult=0;	
				}
			  }  
			  if(currentChoice==0)
			    {
			    blockResult=tempBlockResult;	
			    playResult[9]=blockers[0][0];
				playResult[10]=blockers[0][1];
				playResult[34]=blockers[0][2];
				playResult[35]=blockers[0][3];
				playResult[11]=tacklers[0][0];
				playResult[12]=tacklers[0][1];
				playResult[26]=tacklers[0][2];
				playResult[27]=tacklers[0][3];
				playResult[20]=tempBlockingWinner; 
				playResult[21]=tempBlockResult;
				playResult[24]=1;
				boolean confidentRunner=false;
				boolean shouldIPitch=false;
				if(option)
				  {
				  System.out.println("Option!!!");	
				  int tempSkills[]=getRunningSkill(offensivePlayers[offensiveSelectedPlayers[positionNumber-1]][1]);
				  if(tempSkills[0]>49)
				    {
					confidentRunner=true;	
					}
				  if((keyQB&&offensiveBlocking!=99)||!confidentRunner||playResult[22]==-1||!optionToKeyed)
					{
					shouldIPitch=true;
					}
				  int correctCall=(int)(50+(offensivePlayers[offensiveSelectedPlayers[positionNumber-1]][4]*2.5)+(offensivePlayers[offensiveSelectedPlayers[positionNumber-1]][10]/4));
				  int randomCall=random.nextInt(100);		
				  if(randomCall<=correctCall)
				    {	
					rightCall=true;	
				    if(!shouldIPitch)
					  {
					  currentChoice=numOfChoices;		
					  } 
					else
					  {
					  optionPitched=true;	
					  playData=nextCard(playData,actionCards,random);
					  shuffledActionCards=playData.getShuffledActionCards();
					  cardData=playData.getCardData();					    	   
					  }
					}	  		    
				  else
					{
					if(shouldIPitch)
					  {
					  currentChoice=numOfChoices;		  
					  }
					else
					  {
					  optionPitched=true;	
					  playData=nextCard(playData,actionCards,random);
					  shuffledActionCards=playData.getShuffledActionCards();
					  cardData=playData.getCardData();	
					  }
					}
				  }
			    else
			      {
				  playResult[13]=blockers[0][0];
				  playResult[14]=blockers[0][1];
				  playResult[36]=blockers[0][2];
				  playResult[37]=blockers[0][3];
				  playResult[15]=tacklers[0][0];
				  playResult[16]=tacklers[0][1];
				  playResult[28]=tacklers[0][2];
				  playResult[29]=tacklers[0][3];
				  playResult[22]=tempBlockingWinner;
				  playResult[23]=tempBlockResult;	
				  if(option)
				    {
					System.out.println("!!!Option");		
					blockResult=tempBlockResult;	
					playResult[1]=offensivePlayers[offensiveSelectedPlayers[optionTo]][0];
					ballCarrier=optionTo+1;
					inside=false;		
				    }
				  else
				    {
				    int correctCall=(int)(50+(offensivePlayers[offensiveSelectedPlayers[positionNumber-1]][4]*2.5)+(offensivePlayers[offensiveSelectedPlayers[positionNumber-1]][10]/4));
				    int randomCall=random.nextInt(100);
				    if(randomCall<=correctCall)
					  {	
					  rightCall=true;
					  }
			        if(tempBlockResult>blockResult)
			          {
			          if(rightCall)
			            {	
			            blockResult=tempBlockResult;
			            if(inside)
			              {
			              inside=false;	
			              }
			            else
			              {
			              inside=true;	  
			              }
					    playResult[24]=2;
					    playResult[25]=1;
			            }
			          else
			            {  
					    playResult[24]=1;	
					    playResult[25]=-1;		  
			            }
			          }
			        else
			          {  
					  if(rightCall)
					    {	
					    playResult[24]=1;
					    playResult[25]=1;
					    }
					  else
					    {  				        	
					    playResult[24]=2;
					    playResult[25]=-1;
					    blockResult=tempBlockResult;
					    if(inside)
						  {
						  inside=false;	
						  }
					    else
						  {
						  inside=true;	  
						  }					  
					    }
			          }  
			        }
			      }
			    }
			  } 
		 playResult[2]=tacklers[playResult[24]-1][0];
		 playResult[3]=tacklers[playResult[24]-1][1];
		 playResult[32]=tacklers[playResult[24]-1][2];
		 playResult[33]=tacklers[playResult[24]-1][3];		 
		 int runningSkills[]=getRunningSkill(offensivePlayers[offensiveSelectedPlayers[positionNumber-1]][1]);
		 int runType=-1;
		 int runNumber=0;
		 playData=nextCard(playData,actionCards,random);
		 shuffledActionCards=playData.getShuffledActionCards();
		 cardData=playData.getCardData();
         if(runAtStunt)
           {
           runMod-=1;
		   }
		 if(shotGun)
		   {
		   runMod+=1;
		   }
		 if(keyNone)
		   {	
		   runMod+=2;
		   }
		 if(runnerKeyed)
		   {
		   runMod+=4;	  	
		   }
		 if(option)
		   {
		   if(!optionPitched&&keyQB)
		     {	
		     runMod+=4;
		     }
		   if(optionPitched&&optionToKeyed)
			 {	
			 runMod+=4;
			 }		     
		   }
		 runNumber=shuffledActionCards[cardData[0]][1];
		 playResult[40]=shuffledActionCards[cardData[0]][2];
		 if(offensiveBlocking==99)
		   {
		   runMod=0;;	  
		   }
		 runNumber+=runMod;  
		 if(runNumber<1)
		   {
		   runNumber=1;	
		   }
		 if(runNumber>12)
		   {
		   runNumber=12;	  
		   }
		 if(inside)
		   {
		   runType=0;	
		   }
		 else
		   {
		   runType=1;
		   }
		 if(runNumber==1&&offensiveBlocking!=99)
		   {
		   playData=nextCard(playData,actionCards,random);
		   shuffledActionCards=playData.getShuffledActionCards();
		   cardData=playData.getCardData();	
		   runNumber=shuffledActionCards[cardData[0]][1];
		   playResult[40]=shuffledActionCards[cardData[0]][2];
		   runType=2;	
		   blockResult=0;
		   }
		 if(offensiveBlocking==99)
		   {
		   runType=3;
		   blockResult=0;	  
		   }
		 int runningValue=getSkill(false,offensiveSelectedPlayers[positionNumber-1],offensivePlayers,runningSkills[runType],runNumber,true)+blockResult;
		 if(runningValue>ballOn)
		   {
		   runningValue=ballOn;	
		   }
		 if(inside&&runningValue<-3)
		   {
		   runningValue=-3;	  
		   }
		 if((runningValue*-1)+ballOn>100)
		   {
		   runningValue=(100-ballOn)*-1;
		   }		   
	     playResult[6]=runningValue;
	     if(playResult[40]==1)
	       {
		   playResult[2]=0;
		   playResult[3]=0;	
	       }
         }//End Of Run 
		if(offensivePlay[4]==9)
		  {
		  //Reverse	
		  }
	   if((offensivePlay[4]>=10&&offensivePlay[4]<=15&&offensivePlay[4]!=12)||offensivePlay[4]==39)
		 {
         //Pass		
		 boolean passRush=false;
		 boolean quickPass=false;
		 boolean shortPass=false;
		 boolean longPass=false;
		 boolean downPass=false;
		 boolean swingPass=false;
		 boolean shovelPass=false;
		 boolean thrownAway=false;
		 boolean spreadReceivers=false;
		 boolean deepRoute=false;
		 boolean endZoneRoute=false;
		 boolean leBlocking=false;
		 boolean reBlocking=false;
		 boolean bk1Blocking=false;
		 boolean bk2Blocking=false;
		 boolean bk3Blocking=false;
		 boolean playAction=false;
		 for(int currentStrategy=6;currentStrategy<16;currentStrategy++)
			{
			if(offensivePlay[currentStrategy]==5)
			  {
			  playAction=true;	
			  System.out.println("playAction");
			  }
			if(offensivePlay[currentStrategy]==7)
			  {
			  spreadReceivers=true;	
			  System.out.println("Spread Receivers");
			  }	
			if(offensivePlay[currentStrategy]==8)
			  {
			  deepRoute=true;	
			  System.out.println("deepRoute");
			  }	
			if(offensivePlay[currentStrategy]==9)
			  {
			  endZoneRoute=true;	
			  System.out.println("endZoneRoute");
			  }				
			if(offensivePlay[currentStrategy]==10)
			  {
			  leBlocking=true;	
			  }
			if(offensivePlay[currentStrategy]==11)
			  {
			  reBlocking=true;
			  }	
			if(offensivePlay[currentStrategy]==12)
			  {
			  bk1Blocking=true;	
			  }	
			if(offensivePlay[currentStrategy]==13)
			  {
			  bk2Blocking=true;	
			  }	
			if(offensivePlay[currentStrategy]==14)
			  {
			  bk3Blocking=true;	
			  }				  		  		  
			}
		 for(int currentPlayer=2;currentPlayer<7;currentPlayer++)
		    {
		    if(offensiveSelectedPlayers[currentPlayer]!=-1)
		      {	
			  int blockingSkill=getBlockingSkill(offensivePlayers[offensiveSelectedPlayers[currentPlayer]][1],false);
			  int blockingValue=getSkill(false,offensiveSelectedPlayers[currentPlayer],offensivePlayers,blockingSkill,1,true);
			  passBlockTotal+=blockingValue;		            	
		      }
		    }
         if(leBlocking)
           {
           int le=0;	
		   if(offensiveSelectedPlayers[1]!=-1)
			 {
			 le=1;		
			 }
		   int blockingSkill=getBlockingSkill(offensivePlayers[offensiveSelectedPlayers[le]][1],false);
		   int blockingValue=getSkill(false,offensiveSelectedPlayers[le],offensivePlayers,blockingSkill,1,true);
		   passBlockTotal+=blockingValue;
		   }
		 if(reBlocking)
		   {
		   int re=7;	
		   if(offensiveSelectedPlayers[8]!=-1)
			 {
			 re=8;		
			 }
		   int blockingSkill=getBlockingSkill(offensivePlayers[offensiveSelectedPlayers[re]][1],false);
		   int blockingValue=getSkill(false,offensiveSelectedPlayers[re],offensivePlayers,blockingSkill,1,true);
		   passBlockTotal+=blockingValue;
		   }	
		 for(int currentPlayer=13;currentPlayer<16;currentPlayer++)
			{
			if(offensiveSelectedPlayers[currentPlayer]!=-1&&offensivePlayers[offensiveSelectedPlayers[currentPlayer]][1]!=14)
			  {	
			  int blockingSkill=getBlockingSkill(offensivePlayers[offensiveSelectedPlayers[currentPlayer]][1],false);
			  int blockingValue=getSkill(false,offensiveSelectedPlayers[currentPlayer],offensivePlayers,blockingSkill,1,true);
			  passBlockTotal+=blockingValue;		            	
			  if(currentPlayer==15)
				{
				System.out.println("BK"+(currentPlayer-12)+" Blocking="+passBlockTotal);		
				}
			  }
		    }   		   	   
         if(bootLeg)
           {
		   passBlockTotal+=2;
		   }
		 if(shotGun)
		   {
		   passBlockTotal++;
		   }		   
         if(playAction)
           {
		   System.out.println("Total playaction pre Blocking="+passBlockTotal);	
		   passBlockTotal--;
		   System.out.println("Total playaction pos Blocking="+passBlockTotal);	
           }
 	     for(int currentPlayer=0;currentPlayer<21;currentPlayer++)
		   {
		   if(currentPlayer<10&&defensiveSelectedPlayers[currentPlayer]!=-1)
			 {	
			 int blockingSkill=getBlockingSkill(defensivePlayers[defensiveSelectedPlayers[currentPlayer]][1],false);
			 int blockingValue=getSkill(false,defensiveSelectedPlayers[currentPlayer],defensivePlayers,blockingSkill,1,true);
             if(doubleTeamDefender&&doubleTeamPosition==currentPlayer)
               {
			   blockingValue/=2;
               }
			 passRushTotal+=blockingValue;	
			 int deflectSkill=getDeflectSkill(defensivePlayers[defensiveSelectedPlayers[currentPlayer]][1]);
			 int deflectValue=getSkill(false,defensiveSelectedPlayers[currentPlayer],defensivePlayers,blockingSkill,1,true);
			 if(doubleTeamDefender&&doubleTeamPosition==currentPlayer)
			   {
			   deflectValue/=2;
			   }
			 passDeflectTotal+=deflectValue;	
			 }
		   if(currentPlayer>9&&currentPlayer<15&&defensiveSelectedPlayers[currentPlayer]!=-1&&blitzers[currentPlayer-10])
		     {	 
		     //LB Blitz
		     if(currentPlayer!=10&&currentPlayer!=12&&currentPlayer!=14)
		       {
		       System.out.println("LB Blitz["+currentPlayer+"] Pre="+passRushTotal);
		       }
			 int blockingSkill=getBlockingSkill(defensivePlayers[defensiveSelectedPlayers[currentPlayer]][1],false);
			 int blockingValue=getSkill(false,defensiveSelectedPlayers[currentPlayer],defensivePlayers,blockingSkill,1,true);
			 passRushTotal+=blockingValue;
			 if(currentPlayer!=10&&currentPlayer!=12&&currentPlayer!=14)
			   {
			   System.out.println("LB Blitz["+currentPlayer+"] Pos="+passRushTotal);
			   }	
		     }
		   if(currentPlayer==15&&defensiveSelectedPlayers[currentPlayer]!=-1&&blitzers[5])
			 {	 
			 //LCB Blitz
			 System.out.println("LCB Blitz["+currentPlayer+"] Pre="+passRushTotal);
			 int blockingSkill=getBlockingSkill(defensivePlayers[defensiveSelectedPlayers[currentPlayer]][1],false);
			 int blockingValue=getSkill(false,defensiveSelectedPlayers[currentPlayer],defensivePlayers,blockingSkill,1,true);
			 passRushTotal+=blockingValue;
			 System.out.println("LCB Blitz["+currentPlayer+"] Pos="+passRushTotal);	
			 }
		   if(currentPlayer==16&&defensiveSelectedPlayers[currentPlayer]!=-1&&blitzers[6])
			 {	 
			 //NCB Blitz
			 System.out.println("NCB Blitz["+currentPlayer+"] Pre="+passRushTotal);
			 int blockingSkill=getBlockingSkill(defensivePlayers[defensiveSelectedPlayers[currentPlayer]][1],false);
			 int blockingValue=getSkill(false,defensiveSelectedPlayers[currentPlayer],defensivePlayers,blockingSkill,1,true);
			 passRushTotal+=blockingValue;
			 System.out.println("NCB Blitz["+currentPlayer+"] Pos="+passRushTotal);	
			 }
		   if(currentPlayer==17&&defensiveSelectedPlayers[currentPlayer]!=-1&&blitzers[10])
			 {	 
			 //DCB Blitz
			 System.out.println("DCB Blitz["+currentPlayer+"] Pre="+passRushTotal);
			 int blockingSkill=getBlockingSkill(defensivePlayers[defensiveSelectedPlayers[currentPlayer]][1],false);
			 int blockingValue=getSkill(false,defensiveSelectedPlayers[currentPlayer],defensivePlayers,blockingSkill,1,true);
			 passRushTotal+=blockingValue;
			 System.out.println("DCB Blitz["+currentPlayer+"] Pos="+passRushTotal);	
			 }
		   if(currentPlayer==18&&defensiveSelectedPlayers[currentPlayer]!=-1&&blitzers[7])
			 {	 
			 //FS Blitz
			 int blockingSkill=getBlockingSkill(defensivePlayers[defensiveSelectedPlayers[currentPlayer]][1],false);
			 int blockingValue=getSkill(false,defensiveSelectedPlayers[currentPlayer],defensivePlayers,blockingSkill,1,true);
			 passRushTotal+=blockingValue;
			 }
		   if(currentPlayer==19&&defensiveSelectedPlayers[currentPlayer]!=-1&&blitzers[8])
			 {	 
			 //SS Blitz
			 int blockingSkill=getBlockingSkill(defensivePlayers[defensiveSelectedPlayers[currentPlayer]][1],false);
			 int blockingValue=getSkill(false,defensiveSelectedPlayers[currentPlayer],defensivePlayers,blockingSkill,1,true);
			 passRushTotal+=blockingValue;
			 }
		   if(currentPlayer==20&&defensiveSelectedPlayers[currentPlayer]!=-1&&blitzers[9])
			 {	 
			 //RCB Blitz
			 System.out.println("LCB Blitz["+currentPlayer+"] Pre="+passRushTotal);
			 int blockingSkill=getBlockingSkill(defensivePlayers[defensiveSelectedPlayers[currentPlayer]][1],false);
			 int blockingValue=getSkill(false,defensiveSelectedPlayers[currentPlayer],defensivePlayers,blockingSkill,1,true);
			 passRushTotal+=blockingValue;
			 System.out.println("LCB Blitz["+currentPlayer+"] Pos="+passRushTotal);	
			 }
		   }
		 if(fixtureNumber==1)
		   {
		   System.out.println("PassDeflectTotal="+passDeflectTotal);	  
		   }
		 if(stuntLeft)
		   {
		   passRushTotal+=2;
		   }
		 if(stuntRight)
		   {
		   passRushTotal+=2;
		   }		   
		 int primaryReceiver=offensiveSelectedPlayers[offensivePlay[3]-1];
		 int secondaryReceiver=0;
		 playResult[9]=offensivePlayers[primaryReceiver][0];
		 if(defensivePlay[4]==23&&!quickPass&&!shovelPass)
		   {
		   passRush=true;
		   }
		 if(!passRush)
		   {
		   //Primary Pass	
		   int whoToItem=0;	
		   if(offensivePlay[4]==10)
			 {	
			 swingPass=true;
			 whoToItem=82;
			 }
		   if(offensivePlay[4]==11)
		     {	
		     shovelPass=true;
		     whoToItem=81;
			 }
		   if(offensivePlay[4]==13)
			 {	
			 quickPass=true;
			 whoToItem=81;
			 }
		   if(offensivePlay[4]==14)
		     {
		     shortPass=true;
			 whoToItem=82;
			 }
		   if(offensivePlay[4]==15)
		     {
		     longPass=true;
			 whoToItem=83;	
			 }
		   if(offensivePlay[4]==39)
		     {
		     downPass=true;
			 if(toGo<10)
			   {
			   whoToItem=81;	
			   }
			 if(toGo>9&&toGo<20)
			   {
			   whoToItem=82;
			   }
			 if(toGo>19)
			   {
			   whoToItem=83;
			   }		  
			 }
		   int whoTo=shuffledActionCards[cardData[0]][whoToItem];
		   if(whoTo==-1)
		     {
		     if(bootLeg)
		       {
		       if(passRushTotal>passBlockTotal)
				 {
				 passRush=true;	
				 System.out.println("PassRushWins="+passRush);
				 }
			   else
				 {   
				 whoTo=0;	
				 }
			   }
		     else
		       {  
		       passRush=true;
		       }	
			 }
		   if(whoTo==0)
		     {
			 if((offensivePlay[3]-1==0||offensivePlay[3]-1==1)&&leBlocking)	  
		       {
		       thrownAway=true;	
		       }
			 if((offensivePlay[3]-1==7||offensivePlay[3]-1==8)&&reBlocking)	  
			   {
			   thrownAway=true;	
			   }
			 if(offensivePlay[3]-1==12&&bk1Blocking)	  
			   {
			   thrownAway=true;
			   }
			 if(offensivePlay[3]-1==13&&bk2Blocking)	  
			   {
			   thrownAway=true;	
			   }
			 if(offensivePlay[3]-1==14&&bk3Blocking)	  
			   {
			   thrownAway=true;	
			   }
			 if((offensivePlay[3]-1==0||offensivePlay[3]-1==1)&&doubleTeamDefender&&secondaryDoubleTeamer==1)
			   {
			   System.out.println("LE double teaming and primary receiver");	
			   //LE is doubleTeaming
			   thrownAway=true;	  
			   }
			 if((offensivePlay[3]-1==7||offensivePlay[3]-1==8)&&doubleTeamDefender&&secondaryDoubleTeamer==7)
			   {
			   System.out.println("RE double teaming and primary receiver");	
			   //RE is doubleTeaming
			   thrownAway=true;	  
			   }
		     }
	       if(whoTo>0)
		     {
		     //Secondary Pass	
		     if(whoTo==1)
		       {
			   if(leBlocking||swingPass||shovelPass||(doubleTeamDefender&&secondaryDoubleTeamer==1))
		         {
		         thrownAway=true;
		         }
		       else  	
		         {	
		         if(offensiveSelectedPlayers[0]>0)
		           {	
		           //Redirected to LE(WR)	
		           secondaryReceiver=offensiveSelectedPlayers[0];
				   playResult[10]=offensivePlayers[secondaryReceiver][0];	
				   if(primaryReceiver==secondaryReceiver)
				     {  
				     secondaryReceiver=0;	
				     playResult[10]=0;  
				     }
		           }
		         else
		           {  
				   //Redirected to LE(TE)	
			       secondaryReceiver=offensiveSelectedPlayers[1];
				   playResult[10]=offensivePlayers[secondaryReceiver][0];	
				   if(primaryReceiver==secondaryReceiver)
				     {  
				     secondaryReceiver=0;	
				     playResult[10]=0;  
				     }
		           }  
		         }
		       }
			 if(whoTo==2)
			   {
			   if(reBlocking||swingPass||shovelPass||(doubleTeamDefender&&secondaryDoubleTeamer==7))
				  {
				  thrownAway=true;
				  }
				else  	
				  {			   
				  if(offensiveSelectedPlayers[8]>0)
				    {	
				    //Redirected to RE(WR)	
				    secondaryReceiver=offensiveSelectedPlayers[8];
				    playResult[10]=offensivePlayers[secondaryReceiver][0];	
				    if(primaryReceiver==secondaryReceiver)
				      {  
				      secondaryReceiver=0;	
				      playResult[10]=0;  
				      }
				    }
			      else
				    {  
				    //Redirected to RE(TE)	
				    secondaryReceiver=offensiveSelectedPlayers[7];
				    playResult[10]=offensivePlayers[secondaryReceiver][0];	
				    if(primaryReceiver==secondaryReceiver)
				      {  
				      secondaryReceiver=0;	
				      playResult[10]=0;  
				      }
				    }   
				 }
			   }		       
			  if(whoTo==3)
			    {
				if(bk1Blocking)
				  {
				  thrownAway=true;
				  }
				else  	
				  {
				  if(offensiveSelectedPlayers[12]>0)
				    {	
				    //Redirected to BK1	
			        secondaryReceiver=offensiveSelectedPlayers[12];
				    playResult[10]=offensivePlayers[secondaryReceiver][0];	
				    if(primaryReceiver==secondaryReceiver)
				      {  
					  secondaryReceiver=0;	
					  playResult[10]=0;  
					  }
				    }
				  else
				    {  
				    //Redirected to FL2	
				    secondaryReceiver=offensiveSelectedPlayers[11];
				    if(secondaryReceiver!=-1)
				      {
				      playResult[10]=offensivePlayers[secondaryReceiver][0];
				      }	
				    if(primaryReceiver==secondaryReceiver)
				      {  
					  secondaryReceiver=0;	
					  playResult[10]=0;  
					  }
				    if(secondaryReceiver==-1||swingPass||shovelPass)
					  {  
					  secondaryReceiver=0;	
					  playResult[10]=0; 
					  thrownAway=true;
					  }
					}	
			      }
				}		       
			 if(whoTo==4)
			   {
			   if(bk2Blocking)
				 {
				 thrownAway=true;
				 }
			   else  	
				 {
			     if(offensiveSelectedPlayers[13]>0)
				   {	
				   //Redirected to BK2	
				   secondaryReceiver=offensiveSelectedPlayers[13];
				   playResult[10]=offensivePlayers[secondaryReceiver][0];	
				   if(primaryReceiver==secondaryReceiver)
				     {  
				     secondaryReceiver=0;	
				     playResult[10]=0;  
				     }
				   }
				 else
				   {  
				   //Redirected to FL2	
				   secondaryReceiver=offensiveSelectedPlayers[11];
				   if(secondaryReceiver!=-1)
					 {
					 playResult[10]=offensivePlayers[secondaryReceiver][0];
					 }	
				   if(primaryReceiver==secondaryReceiver)
					 {  
					 secondaryReceiver=0;	
					 playResult[10]=0;  
					 }
				   if(secondaryReceiver==-1||swingPass||shovelPass)
					 { 
					 secondaryReceiver=0;	
					 playResult[10]=0; 
					 thrownAway=true;
					 }
				   } 	
				 }				 
			   }	
		    if(whoTo==5)
			  {
			  if(offensiveSelectedPlayers[10]>0&&!swingPass&&!shovelPass)
			    {	
			    //Redirected to FL1	
			    secondaryReceiver=offensiveSelectedPlayers[10];
			    playResult[10]=offensivePlayers[secondaryReceiver][0];	
			    if(primaryReceiver==secondaryReceiver)
				  {  
				  secondaryReceiver=0;	
				  playResult[10]=0;  
			      }
			    }
			  else
			    { 
			    if(offensiveSelectedPlayers[11]>0&&!swingPass&&!shovelPass)
			      {
				  //Redirected to FL2	
			      secondaryReceiver=offensiveSelectedPlayers[11];
			      playResult[10]=offensivePlayers[secondaryReceiver][0];
				  if(primaryReceiver==secondaryReceiver)
				    {  
				    secondaryReceiver=0;	
				    playResult[10]=0;  
				    }
			      }
			    else
			      {
			      //Redirected to BK3	
				  if(bk3Blocking)
				    {
				    thrownAway=true;
				    }
				 else  	
				    {
				    secondaryReceiver=offensiveSelectedPlayers[14];
				    if(secondaryReceiver!=-1)
				      {
				      playResult[10]=offensivePlayers[secondaryReceiver][0];
				      }	
				    if(primaryReceiver==secondaryReceiver)
				      {  
				      secondaryReceiver=0;	
				      playResult[10]=0;  
				      }
				    if(secondaryReceiver==-1||((swingPass||shovelPass)&&offensivePlayers[offensiveSelectedPlayers[14]][1]==14))
				      {  
				      secondaryReceiver=0;	
				      playResult[10]=0; 
				      thrownAway=true;
				      }
				    }  	
			      }
			    }
			  } 	
			}		     
		   }
		 if(passRush)
		   {
		   //System.out.println("PassRush");	  
		   }
		 if(!passRush&&!thrownAway)
		   {
		   int whoTo=0;
		   int numOfDB=0;
		   int complete=0;
		   int coverModifier=0;
		   int dbInt=48;
		   int runNumberMod=0;
		   if(secondaryReceiver==0)
		     {
		     whoTo=primaryReceiver;		  
		     }
		   else
		     {
			 whoTo=secondaryReceiver;
			 if(spreadReceivers)
			   {
			   System.out.println("Spreading Receivers");
			   quickPass=false;
			   shortPass=false;
			   longPass=false;
			   downPass=false;
			   swingPass=false;
			   shovelPass=false;			   
			   if(offensivePlayers[whoTo][1]==12)
			     {	
		         //Secondary receiver is TE, route is short.
		         shortPass=true;
			     }
			   if(offensivePlayers[whoTo][1]==13)
				 {	
				 //Secondary receiver is WR, route is long.
				 longPass=true;
				 }
			   if(offensivePlayers[whoTo][1]==15)
				 {	
				 //Secondary receiver is RB, route is quick.
				 quickPass=true;
				 }				 
			   }
		     }
		   boolean receiverFound=false; 
		   if(shovelPass)
		     {
		     receiverFound=true;	
		     }
		   for(int currentPlayer=0;currentPlayer<coverageTable.length&&!receiverFound;currentPlayer++)
              {
              if(coverageTable[currentPlayer][1]==whoTo)
                {
                boolean wr=false;
                boolean te=false;
                if(offensivePlayers[whoTo][1]==12)
                  {
                  te=true;		
                  }
				if(offensivePlayers[whoTo][1]==13)
				  {
				  wr=true;		
				  }                  
                receiverFound=true;
                if(coverageTable[currentPlayer][2]!=-1&&!amIBlitzing(coverageTable[currentPlayer][2],defensiveSelectedPlayers,blitzers))
                  {
				  int coverSkills[]=getCoverSkills(defensivePlayers[coverageTable[currentPlayer][2]][1]);
				  coverModifier+=getSkill(false,coverageTable[currentPlayer][2],defensivePlayers,coverSkills[0],1,true);  
				  dbInt-=getSkill(false,coverageTable[currentPlayer][2],defensivePlayers,coverSkills[1],1,true);	
                  numOfDB++;
                  if(defensivePlayers[coverageTable[currentPlayer][2]][1]==19||defensivePlayers[coverageTable[currentPlayer][2]][1]==20)
                    {
                    if(wr)
                      {	
                      System.out.println("LB1a");
                      coverModifier+=2;
                      }
                    if(te)  
					  {	
					  coverModifier++;
					  }                    
                    }  
				  }
                if(coverageTable[currentPlayer][3]!=-1&&!amIBlitzing(coverageTable[currentPlayer][3],defensiveSelectedPlayers,blitzers))
				  {
				  int coverSkills[]=getCoverSkills(defensivePlayers[coverageTable[currentPlayer][2]][1]);
				  coverModifier+=getSkill(false,coverageTable[currentPlayer][2],defensivePlayers,coverSkills[0],1,true);  
				  dbInt-=getSkill(false,coverageTable[currentPlayer][2],defensivePlayers,coverSkills[1],1,true);	
				  numOfDB++;
				  if(defensivePlayers[coverageTable[currentPlayer][2]][1]==19||defensivePlayers[coverageTable[currentPlayer][2]][1]==20)
					{
					if(wr)
					  {	
					  System.out.println("LB2a");
					  coverModifier+=2;
					  }
					if(te)  
					  {	
					  System.out.println("LB2b");	
					  coverModifier++;
					  }      
					}
				  }
				if(coverageTable[currentPlayer][4]!=-1&&!amIBlitzing(coverageTable[currentPlayer][3],defensiveSelectedPlayers,blitzers))
				  {
				  int coverSkills[]=getCoverSkills(defensivePlayers[coverageTable[currentPlayer][2]][1]);
				  coverModifier+=getSkill(false,coverageTable[currentPlayer][2],defensivePlayers,coverSkills[0],1,true);  
				  dbInt-=getSkill(false,coverageTable[currentPlayer][2],defensivePlayers,coverSkills[1],1,true);	
				  numOfDB++;
				  if(defensivePlayers[coverageTable[currentPlayer][2]][1]==19||defensivePlayers[coverageTable[currentPlayer][2]][1]==20)
					{
					if(wr)
					  {	
					  System.out.println("LB3a");
					  coverModifier+=2;
					  }
					if(te)  
					  {	
					  System.out.println("LB3b");	
					  coverModifier++;
					  }      	
					}
				  }
                }
              }    
			if(numOfDB==0&&!shovelPass)
			  {
			  complete=+5;
			  runNumberMod=-1;
			  dbInt=50;
			  }
			if(numOfDB==2)
			  {
              coverModifier=-7;
              dbInt=44;
              }
			if(numOfDB==3)
			  {
			  coverModifier=-15;
			  dbInt=40;
			  }	
			if(dbInt>50)
			  {
			  dbInt=50;	  			  
			  }
			if(shovelPass)
			  { 
			  coverModifier+=5;	
			  }
			if(downPass)
			  {
			  if(coverModifier>0)
			    {
			    coverModifier/=2;		  
			    }
			  else
			    {
			    coverModifier*=2;	  
			    }
			  }
			if(playAction)
			  {
			  System.out.println("PlayAction");	  
			  }
			if(playAction&&(keyFB1||keyFB2||keyRB1||keyRB2||keyQB||keyNone))
			  {
			  System.out.println("playAction vs Run pre="+coverModifier);	
			  coverModifier+=5;
			  System.out.println("playAction vs Run pos="+coverModifier);	  
			  }
			if(playAction&&(passDefense||preventDefense))
			  {
			  System.out.println("playAction vs Pass pre="+coverModifier);	
			  coverModifier+=-5;
			  System.out.println("playAction vs Pass pos="+coverModifier);	  
			  }				  
			if(quickPass||(downPass&&toGo<10))
			  {
			  if(keyFB1||keyFB2||keyRB1||keyRB2||keyQB||keyNone)
			    {	
			    if(ballOn<20)
			      {
			      coverModifier+=-10;		  
			      }
			    }
			  if(passDefense)
			    {
				if(ballOn<20)
				  {
				  coverModifier+=-15;		  
				  }
				else
				  { 
				  coverModifier+=-10;	 				    	    
				  }
			    }
			  if(blitz)
			    {
				coverModifier+=10;		
			    }
			  }
			if(shortPass||swingPass||(downPass&&toGo>9&&toGo<20))
			  {  
			  if(keyFB1||keyFB2||keyRB1||keyRB2||keyQB||keyNone)
				{	
				if(ballOn>19)
				  { 
				  coverModifier+=5;	
				  }
				}
			  if(passDefense)
				{
			    coverModifier+=-5;	
				}
			  if(shortPass&&preventDefense)
			    {
				coverModifier+=-5;	
				System.out.println("shortPass vs prevent"); 	
				}
			  if(swingPass&&preventDefense)
				{
				coverModifier+=5;	
				System.out.println("swingPass vs prevent"); 	
				}					
			  }
			if(longPass||(downPass&&toGo>19))
			  {  
			  if(keyFB1||keyFB2||keyRB1||keyRB2||keyQB||keyNone)
				{
				coverModifier+=5;
				}
			  if(preventDefense)
				{
				coverModifier+=-7;	
				System.out.println("longPass vs prevent"); 	
				}
			  }
			int qbPosition=0;
			int qbInt=0;
			if(!shotGun)
			  {
			  qbPosition=9;
			  }
			else
			  {
			  qbPosition=14;
			  }
			int passingSkills[]=getPassingSkills(offensivePlayers[offensiveSelectedPlayers[qbPosition]][1]);
			int passNumber=shuffledActionCards[cardData[0]][3];
			if(quickPass||shovelPass||(downPass&&toGo<10))
			  {
			  complete+=getSkill(false,offensiveSelectedPlayers[qbPosition],offensivePlayers,passingSkills[0],1,true); 
			  if(quickPass||downPass)
			    {
			    qbInt=48-getSkill(false,offensiveSelectedPlayers[qbPosition],offensivePlayers,passingSkills[1],1,true);
			    }
			  else
			    {
			    qbInt=49;	  
			    }
			  }
			if(shortPass||swingPass||(downPass&&toGo>9&&toGo<20))
			  {
			  complete+=getSkill(false,offensiveSelectedPlayers[qbPosition],offensivePlayers,passingSkills[2],1,true); 
			  qbInt=48-getSkill(false,offensiveSelectedPlayers[qbPosition],offensivePlayers,passingSkills[1],1,true);
			  if(swingPass)
			    {
			    if(qbInt==49)
			      {	
			      qbInt=50;	
			      }
			    if(qbInt<49)
			      {  
			      qbInt=49;	
			      }
			    }
			  }
			if(longPass||(downPass&&toGo>19))
			  {
			  complete+=getSkill(false,offensiveSelectedPlayers[qbPosition],offensivePlayers,passingSkills[4],1,true); 
			  qbInt=48-getSkill(false,offensiveSelectedPlayers[qbPosition],offensivePlayers,passingSkills[1],1,true);
			  }  			      
			if(endZoneRoute)
			  {
			  System.out.println("endZoneRoutePre="+complete);	
			  complete+=-8;
			  System.out.println("endZoneRoutePos="+complete);	
			  }
			else
			  {	
			  if(deepRoute)
                {
                System.out.println("DeepRoutePre="+complete);	
                complete+=-4;
			    System.out.println("DeepRoutePos="+complete);	
                }
			  }
			if(leBlocking)
              {
              complete++;	
			  }
			if(reBlocking)
			  {
			  complete++;	
			  }
			if(bk1Blocking)
			  {
			  complete++;	
			  }
			if(bk2Blocking)
			  {
			  complete++;	
			  }
			if(bk3Blocking)
			  {
			  complete++;	
			  }  
            if(bootLeg)
              {
              complete-=2;	
			  }
            if(shotGun&&shortPass)
              {
              System.out.println("shotGunShort Pre="+complete);	
              complete+=2;
			  System.out.println("shotGunShort Pos="+complete);		
              }
			if(shotGun&&longPass)
			  {
			  System.out.println("shotGunLong Pre="+complete);	
			  complete++;
			  System.out.println("shotGunLong Pos="+complete);		
			  }                  
			complete+=coverModifier;
			boolean completePass=false;
			boolean passInterceptedQB=false; 
			boolean passInterceptedDB=false;
			if(thrownAway)
			  {
			  playResult[62]=11;	 
			  }
            if(passNumber<=complete&&passNumber>=dbInt)
			  {
			  System.out.println("Tussle for Ball");	
			  }
			else
			  { 
			  if(passNumber<=complete)
				{
				//Pass Complete
				completePass=true;		 
				}
			  else
				{  
				if(passNumber>=qbInt||(passNumber==48&&qbInt==49&&random.nextInt(2)==0)) 
				  {
				  //Pass Intercepted By QB.	
				  if(passNumber==48&&qbInt==49)
				    {
				    playResult[62]=10;
					}
				  passInterceptedQB=true;
				  }
				else
				  {	
				  if(passNumber>=dbInt||(passNumber==48&&dbInt==49&&random.nextInt(2)==0))
				    {	
				    //Pass Intercepted by DB.	
					if(passNumber==48&&dbInt==49)
					  {
					  System.out.println("DBIntercepted49");
					  playResult[62]=10;		
					  }
					passInterceptedDB=true;	    
				    }
				  else
					{
					if(passNumber<=(complete-coverModifier))
					  {
					  //Incomplete(Pass Defended)
					  playResult[62]=10;			
					  }
					else
					  { 
					  int missedBy=passNumber-complete;
					  int missedType=random.nextInt(3);
					  if(missedBy<11)
					    {
					    //Just		 
					    if(missedType==0)
						  {
						  //Off-Target	
						  playResult[62]=1;	
						  }
						if(missedType==1)
						  {
						  //Under-Thrown	
						  playResult[62]=2;	  
						  } 
						if(missedType==2)
						  {
                          //Over-Thrown	
						  playResult[62]=3;		  
						  } 
					    }
					  else
					    {  
						if(missedBy>29)
						  {
						  //Badly	  
						  if(missedType==0)
							{
							//Off-Target	
							playResult[62]=7;
							}
						  if(missedType==1)
							{
							//Under-Thrown	
							playResult[62]=8;	  
							} 
						  if(missedType==2)
							{
							//Over-Thrown	
							playResult[62]=9;	  
							} 		
						  }
						else
						  {  
						  //Normal	  
						  if(missedType==0)
							{
							//Off-Target	
							playResult[62]=4;
							}
						  if(missedType==1)
							{
							//Under-Thrown	
							playResult[62]=5;	  
							} 
						  if(missedType==2)
							{
							//Over-Thrown	
							playResult[62]=6;	  
							} 		
						  }
					    }
					  }	
					}
				  }  
				}  
			  }
           if(!receiverFound)
			 {
			 System.out.println("Not Found");	  
			 }
           if(completePass)
             {
             //PassComplete	
			 int receivingSkills[]=getReceivingSkill(offensivePlayers[whoTo][1]);
			 int receivingSkill=-1;
			 if(quickPass)
			   {
			   receivingSkill=0;	
			   }
			 if(shortPass)
			   {
			   receivingSkill=1;	
			   }
			 if(swingPass)
			   {
			   receivingSkill=1;	
			   }
			 if(shovelPass)
			   {
			   receivingSkill=0;	
			   }										
			 if(longPass)
			   {
			   receivingSkill=2;	
			   }
			 if(downPass)
			   {
			   if(toGo<10)
			     {
				 receivingSkill=0;	
				 }
			   if(toGo>9&&toGo<20)
				 {
				 receivingSkill=1;	
				 }
			   if(toGo>19)
				 {
				 receivingSkill=2;	
				 System.out.println("CP6C="+receivingSkill);		
				 }				 			     
			   }	
		     int runNumber=0;
             runMod=0;
             int ydsMod=0;
		     playData=nextCard(playData,actionCards,random);
		     shuffledActionCards=playData.getShuffledActionCards();
		     cardData=playData.getCardData();
             if(numOfDB==0)
               {
			   runMod-=1;
			   }
		     if(swingPass)
			   {
			   runMod-=2;
			   ydsMod-=5;
			   }
			 if(shovelPass)
			   {
			   System.out.println("shovelPass YdsModPre="+ydsMod);	
			   ydsMod-=2;
			   System.out.println("shovelPass YdsModPos="+ydsMod);
			   }			   
		     if(deepRoute)
			   {
			   System.out.println("deepRoute RunModPre="+runMod);	
			   runMod-=1;
			   System.out.println("deepRoute RunModPos="+runMod);
			   }
			 if(endZoneRoute)
			   {
			   System.out.println("endZoneRoute RunModPre="+runMod);	
			   runMod-=2;
			   System.out.println("endZoneRoute RunModPos="+runMod);
			   }			   
		     runNumber=shuffledActionCards[cardData[0]][1];
		     playResult[40]=shuffledActionCards[cardData[0]][2];
		     runNumber+=runMod;  
		     if(runNumber<1)
			   {
			   runNumber=1;	
			   }
		     if(runNumber>12)
			   {
			   runNumber=12;	  
			   }
		     if(runNumber==1)
			   {
			   playData=nextCard(playData,actionCards,random);
			   shuffledActionCards=playData.getShuffledActionCards();
			   cardData=playData.getCardData();	
			   runNumber=shuffledActionCards[cardData[0]][1];
			   playResult[40]=shuffledActionCards[cardData[0]][2];
			   receivingSkill=2;
			   }
		     int receivingValue=getSkill(false,whoTo,offensivePlayers,receivingSkills[receivingSkill],runNumber,true)+ydsMod;
		     if(receivingValue>ballOn)
   			   {
			   receivingValue=ballOn;	
			   }
		     if((receivingValue*-1)+ballOn>100)
			   {
			   receivingValue=(100-ballOn)*-1;
			   }		   
			 playResult[6]=receivingValue;
		     if(playResult[40]==1)
			   {
			   playResult[2]=0;
			   playResult[3]=0;	
			   } 
             }
		   if(passInterceptedQB)
			 {  
			 //System.out.println("PassIntQB");	
			 }             
           if(passInterceptedDB)
             {  
			 //System.out.println("PassIntDB");	
             }
           }
	   }  
	   if(offensivePlay[4]==29||offensivePlay[4]==30)
		 {
		 //Punt	  
		 offensivePlayFound=true;
		 defensivePlayFound=true;
		 playResult[0]=offensivePlayers[offensiveSelectedPlayers[14]][0];  
		 int puntBlockModifier=specialTeamsModifier(false,true,false,offensiveSelectedPlayers,defensiveSelectedPlayers,offensivePlayers,defensivePlayers)*-1;  
		 int puntingSkills[]=getPuntingSkills(offensivePlayers[offensiveSelectedPlayers[14]][1]);
		 int puntingSkill=puntingSkills[0];
		 int returnedSkill=puntingSkills[2];
		 int blockedSkill=puntingSkills[1];
		 int runNumber=shuffledActionCards[cardData[0]][1];
		 int kickYards=0;
		 int returnYards=0;
		 int coffinCornerYards=0;
		 if(offensivePlay[4]==30)
		   {
		   coffinCornerYards=10;
		   }
		 boolean puntBlocked=false;
		 boolean touchBack=false;
		 boolean returnable=true;
		 boolean greatLine=false;
		 boolean roughingTheKicker=false;
		 boolean movement=false;
		 boolean penalty=false;
		 boolean breakOut=false;
		 if(puntBlockModifier>0)
		   {
		   int randomPick=random.nextInt(2);
		   if(randomPick==0)
			 {	
			 greatLine=true;
			 runNumber=11;
			 puntBlockModifier=0;
			 }
		   }
		 if((runNumber>=12+puntBlockModifier)&&!greatLine)
		   {
		   //Special Punt
		   playData=nextCard(playData,actionCards,random);
		   shuffledActionCards=playData.getShuffledActionCards();
		   cardData=playData.getCardData();	
		   runNumber=shuffledActionCards[cardData[0]][1];
		   if(defensivePlay[4]==37)
		     {
		     playData=nextCard(playData,actionCards,random);
			 shuffledActionCards=playData.getShuffledActionCards();
			 cardData=playData.getCardData();	
			 runNumber=shuffledActionCards[cardData[0]][1];
			 if(runNumber<5)
			   {
			   puntBlocked=true;		
			   }
			 if(runNumber>4&&runNumber<10)
			   {
			   kickYards=getSkill(false,offensiveSelectedPlayers[14],offensivePlayers,puntingSkill,11,true);
			   if(offensivePlay[4]==30&&(runNumber==1||runNumber==3||runNumber==5||runNumber==7||runNumber==9||runNumber==11))
			     {
			     returnable=false;	
			     System.out.println("Coffin Corner Out Of Bounds1");	
			     }
			   }
			 if(runNumber>9)
			   {
			   roughingTheKicker=true;	
			   penalty=true;
			   }			   			   
		     }
		   else
		     {  
		     int blocked=getSkill(false,offensiveSelectedPlayers[14],offensivePlayers,blockedSkill,1,true);
		     if(runNumber>=13-blocked)
		       {
				int randomPick=random.nextInt(2);
				if(randomPick==0)
				  {	
				  puntBlocked=true;
		          }
			    else
			      {
			      movement=true;	
			      penalty=true;
				  }
		       }
		     else
		       {
		       //Long Punt
			   kickYards=getSkill(false,offensiveSelectedPlayers[14],offensivePlayers,puntingSkill,1,true)+runNumber;
			   returnable=false;
			   playResult[9]=99;  
		       }
		     }  
		   }
		 else
		   {
		   //Standard Punt	  
		   kickYards=getSkill(false,offensiveSelectedPlayers[14],offensivePlayers,puntingSkill,runNumber,true);
		   if(offensivePlay[4]==30&&(runNumber==1||runNumber==3||runNumber==5||runNumber==7||runNumber==9||runNumber==11))
			 {
			 returnable=false;	
			 }
		   }
		 if(kickYards>0&&coffinCornerYards>0)
		   {
		   kickYards-=coffinCornerYards;	
		   if(kickYards>=ballOn)
		     {
			 int puntSkill=getSkill(false,offensiveSelectedPlayers[14],offensivePlayers,puntingSkill,0,false);
			 if(puntSkill>80)
			   {
			   puntSkill=80;	
			   }
			 if(puntSkill<20)
			   {
			   puntSkill=20;	
			   }			   
			 int randomPick=random.nextInt(101);
			 if(randomPick<=puntSkill)
			   {	
			   kickYards=ballOn-(random.nextInt(10)+1);	
			   }
		     }
		   }
		 if(kickYards>=ballOn)
		   {
		   kickYards=ballOn;  
		   touchBack=true;	  
		   }
         if((offensivePlay[4]==29||offensivePlay[4]==30)&&!touchBack&&kickYards>ballOn-6)
           {
           playData=nextCard(playData,actionCards,random);
		   shuffledActionCards=playData.getShuffledActionCards();
		   cardData=playData.getCardData();
		   runNumber=shuffledActionCards[cardData[0]][1];
		   if(ballOn-kickYards-runNumber>0)
		     {
		     returnable=false;
			 }
		   else
		     {
			 playResult[11]=ballOn-kickYards;	
		     kickYards=ballOn;
		     touchBack=true;
		     }
           }
		 if(puntBlocked)
		   {  
		   playResult[39]=whoBlockedOrRecoveredTheKick(false,false,true,true,defensiveSelectedPlayers,defensivePlayers,random);
		   playData=nextCard(playData,actionCards,random);
		   shuffledActionCards=playData.getShuffledActionCards();
		   cardData=playData.getCardData();
		   kickYards=(shuffledActionCards[cardData[0]][1]*-1)+-10;
		   if(ballOn+(kickYards*-1)>100)
		     {
		     kickYards=(100-ballOn)*-1;	
		     }
		   int whoRecovered=32+(puntBlockModifier*-1);
		   if(whoRecovered<22)
			 {
			 whoRecovered=22;	
		     }
		   if(whoRecovered>42)
		     {
		     whoRecovered=42;	  
		     }
		   playData=nextCard(playData,actionCards,random);
		   shuffledActionCards=playData.getShuffledActionCards();
		   cardData=playData.getCardData();
		   int recoverNumber=shuffledActionCards[cardData[0]][3];
		   if(recoverNumber>whoRecovered)
		     {
		     //Offense Recovers	
			 playResult[4]=whoBlockedOrRecoveredTheKick(false,false,true,false,offensiveSelectedPlayers,offensivePlayers,random);	
			 playData=nextCard(playData,actionCards,random);
		     shuffledActionCards=playData.getShuffledActionCards();
			 cardData=playData.getCardData();
		     int returnNumber=shuffledActionCards[cardData[0]][1];
		     playResult[6]=getSkill(true,0,offensivePlayers,78,returnNumber,true);
		     if(returnNumber==1)
		       {
		       playData=nextCard(playData,actionCards,random);
		       shuffledActionCards=playData.getShuffledActionCards();
		       cardData=playData.getCardData();
			   returnNumber=shuffledActionCards[cardData[0]][1];
			   int breakOutYards=getSkill(true,0,offensivePlayers,79,returnNumber,true);	
			   if(breakOutYards>0)
				 {		
				 playResult[6]=breakOutYards;	 
				 }
               }
			 if(playResult[6]!=0)
			   {
			   playResult[6]+=random.nextInt(11)-5;
			   }
			 if(ballOn+(kickYards*-1)-playResult[6]<0)
			   {
			   playResult[6]=ballOn+(kickYards*-1);
			   }
			 if(ballOn+(kickYards*-1)-playResult[6]>100)
			   {
			   playResult[6]=100-(ballOn+(kickYards*-1));	  
			   }				   
			 }
		   else
			 {
             //Defense Recovers
			 playResult[31]=1;	
			 playResult[4]=whoBlockedOrRecoveredTheKick(false,false,true,false,defensiveSelectedPlayers,defensivePlayers,random);	
		     playData=nextCard(playData,actionCards,random);
		     shuffledActionCards=playData.getShuffledActionCards();
		     cardData=playData.getCardData();
			 int returnNumber=shuffledActionCards[cardData[0]][1];
		     playResult[7]=getSkill(true,0,defensivePlayers,78,returnNumber,true);
             if(returnNumber==1)
               {
			   playData=nextCard(playData,actionCards,random);
		       shuffledActionCards=playData.getShuffledActionCards();
		       cardData=playData.getCardData();
		       returnNumber=shuffledActionCards[cardData[0]][1];
		       int breakOutYards=getSkill(true,0,offensivePlayers,79,returnNumber,true);	
		       if(breakOutYards>0)
			     {		
			     playResult[7]=breakOutYards;	 
			     }
               }
			 if(playResult[7]!=0)
			   {
			   playResult[7]+=random.nextInt(11)-5;
			   }	
			 if(ballOn+(kickYards*-1)+playResult[7]>100)
			   {
			   playResult[7]=100-(ballOn+(kickYards*-1));	  
			   }
			 if(ballOn+(kickYards*-1)+playResult[6]<0)
			   {
			   playResult[6]=100-(ballOn+(kickYards*-1));	
			   touchBack=true;  
			   }
			 }
		   }
		 if(!puntBlocked&&returnable&&!touchBack&&!penalty)
		   {
		   boolean shouldHaveBeenAFairCatch=false;	
		   boolean fairCatch=false;	
		   playResult[5]=defensivePlayers[defensiveSelectedPlayers[18]][0];
		   int returned=getSkill(false,offensiveSelectedPlayers[14],offensivePlayers,returnedSkill,1,true);
		   playResult[9]=getSkill(false,offensiveSelectedPlayers[14],offensivePlayers,returnedSkill,1,false);
		   playData=nextCard(playData,actionCards,random);
		   shuffledActionCards=playData.getShuffledActionCards();
		   cardData=playData.getCardData();
		   int fairCatchNumber=shuffledActionCards[cardData[0]][1];
		   if(fairCatchNumber>returned)
		     {
		     fairCatch=true;	
		     shouldHaveBeenAFairCatch=true;
		     }
           if(fairCatch&&defensivePlay[4]==41)
             {
		     int maxReturn=(int)(getSkill(false,defensiveSelectedPlayers[18],defensivePlayers,24,1,false)/2.08);	
			 playData=nextCard(playData,actionCards,random);
			 shuffledActionCards=playData.getShuffledActionCards();
			 cardData=playData.getCardData();
			 int fairCatchOverride=shuffledActionCards[cardData[0]][3];
			 if(fairCatchOverride<=maxReturn)
			   {		
			   fairCatch=false;	     
			   }
             }
		   if(defensivePlay[4]!=36&&defensivePlay[4]!=37&&defensivePlay[4]!=38&&defensivePlay[4]!=41)
			 {
			 returnable=false;
			 System.out.println("Non punt defense called against punt");
			 }
           if(!fairCatch)
             {	
			 playData=nextCard(playData,actionCards,random);
			 shuffledActionCards=playData.getShuffledActionCards();
			 cardData=playData.getCardData();		
			 int returnNumber=shuffledActionCards[cardData[0]][1];
			 if(defensivePlay[4]==38)
			   {
			   returnNumber++;
		       }
			 if(defensivePlay[4]==37)
			   {
			   returnNumber+=2;	
			   System.out.println("Punt called against block defense="+returnNumber);
			   }
			 if(returnNumber>12)
			   {
			   returnNumber=12;	  			    
			   }
			 returnYards=getSkill(false,defensiveSelectedPlayers[18],defensivePlayers,24,returnNumber,true);
			 int coverageMod=specialTeamsModifier(false,true,false,offensiveSelectedPlayers,defensiveSelectedPlayers,offensivePlayers,defensivePlayers);
			 playResult[10]=coverageMod;
			 kickYards+=coverageMod;
			 playResult[40]=shuffledActionCards[cardData[0]][2];
			 if(defensivePlay[4]==41&&shouldHaveBeenAFairCatch)
			   {
			   returnYards/=2;	
			   } 
			else
			   {
			   if(returnNumber==1)
			     {
			     playData=nextCard(playData,actionCards,random);
			     shuffledActionCards=playData.getShuffledActionCards();
			     cardData=playData.getCardData();
			     returnNumber=shuffledActionCards[cardData[0]][1];
			     int chanceOfBreakOut=getSkill(false,defensiveSelectedPlayers[18],defensivePlayers,25,returnNumber,true);	
				 playData=nextCard(playData,actionCards,random);
				 shuffledActionCards=playData.getShuffledActionCards();
				 cardData=playData.getCardData();
				 returnNumber=shuffledActionCards[cardData[0]][1];
			     if(returnNumber<=chanceOfBreakOut)
				   {		
				   returnYards*=2;
				   playResult[40]=shuffledActionCards[cardData[0]][2];
				   breakOut=true;
				   }
				 }
			   else
				 {
				 if(returnNumber==12)
				   {	
				   playResult[30]=1;
			       }	  
				 } 	
			   if(playResult[40]==0)
				 {
				 int puntTacklers[]=specialTeamsTackler(false,false,false,true,breakOut,offensiveSelectedPlayers,offensivePlayers,random);
				 playResult[2]=puntTacklers[0];
				 playResult[3]=puntTacklers[1];
				 }				 			   
               }
             }  
		   }
		 playResult[38]=kickYards;
		 int tempBallOn=100-(ballOn-kickYards);
		 if(kickYards>0&&tempBallOn-returnYards<0)
		   {
		   returnYards=tempBallOn;
		   }
		 if(kickYards>0&&!touchBack&&ballOn-returnYards>99)
		   {  
		   System.out.println("Safety,BallOn="+ballOn+",Kick="+kickYards+",Return="+returnYards);
		   }
		 playResult[8]=returnYards;
         if(penalty)
           {
		   playResult[41]=-1;	
           }
		 }
		if(offensivePlay[4]==12)
		  {
		  //Screen	
		  }
	   if(offensivePlay[4]==27)
		 {
		 //FieldGoal	
		 offensivePlayFound=true;
		 defensivePlayFound=true;
		 playResult[0]=offensivePlayers[offensiveSelectedPlayers[14]][0];  
		 int fgBlockModifier=specialTeamsModifier(false,false,true,offensiveSelectedPlayers,defensiveSelectedPlayers,offensivePlayers,defensivePlayers)*-1;  
		 if(fgBlockModifier<-10)
		   {
		   fgBlockModifier=-10;	
		   }
		 if(fgBlockModifier>10)
		   {
		   fgBlockModifier=10;	  
		   }
		 int kickingSkills[]=getKickingSkills(offensivePlayers[offensiveSelectedPlayers[14]][1]);
		 int kickingSkill=0;
		 int blockingSkill=0;
		 if(extraPointRequired)
		   {
		   kickingSkill=kickingSkills[13];
		   blockingSkill=kickingSkills[14];
		   }
		 else
		   {
		   if((ballOn+17)<=25)
		     {
		     kickingSkill=kickingSkills[3];
			 blockingSkill=kickingSkills[8];		  
		     }
		   if((ballOn+17)>=26&&(ballOn+17)<=35)
			 {
			 kickingSkill=kickingSkills[4];
			 blockingSkill=kickingSkills[9];		  
			 }
		   if((ballOn+17)>=36&&(ballOn+17)<=45)
			 {
			 kickingSkill=kickingSkills[5];
			 blockingSkill=kickingSkills[10];		  
			 }
		   if((ballOn+17)>=46&&(ballOn+17)<=50)
			 {
			 kickingSkill=kickingSkills[6];
			 blockingSkill=kickingSkills[11];		  
			 }
		   if((ballOn+17)>=51)
			 {
			 kickingSkill=kickingSkills[7];
			 blockingSkill=kickingSkills[12];		  
			 }
		   }
		   int kickGood=getSkill(false,offensiveSelectedPlayers[14],offensivePlayers,kickingSkill,1,true);
		   int kickBlocked=getSkill(false,offensiveSelectedPlayers[14],offensivePlayers,blockingSkill,1,true)+fgBlockModifier;
		   if((ballOn+17)>=56)
			 {
			 System.out.println("Over55");	
			 int over55=(ballOn+17)-56;
			 kickGood-=2*over55;
			 kickBlocked+=2*over55;		  
			 }
		   if(kickBlocked<-1)
			 {
			 kickBlocked=-1;	
			 }
		   int passNumber=shuffledActionCards[cardData[0]][3];  
		   boolean kickWasGood=false;
		   boolean kickWasBlocked=false;
		   if(passNumber<=kickGood)
		     {
		     kickWasGood=true;
			 playResult[38]=ballOn+17;	
		     }
		   else
		     {
		     if(passNumber==48&&kickBlocked==-1)
		       {	 
			   playData=nextCard(playData,actionCards,random);
			   shuffledActionCards=playData.getShuffledActionCards();
			   cardData=playData.getCardData();	
			   passNumber=shuffledActionCards[cardData[0]][3];
	           if(passNumber<25)
	             {	
				 kickWasBlocked=true;
	             }
		       }
		     else
		       {
		       if(kickBlocked>-1&&(passNumber>=(48-kickBlocked)))
		         {
		         kickWasBlocked=true;
		         }
		       }
		     if(kickWasBlocked)
		       {
			   playResult[39]=whoBlockedOrRecoveredTheKick(false,true,false,true,defensiveSelectedPlayers,defensivePlayers,random);
			   playData=nextCard(playData,actionCards,random);
			   shuffledActionCards=playData.getShuffledActionCards();
			   cardData=playData.getCardData();
			   playResult[38]=shuffledActionCards[cardData[0]][1]*-1;
		       int whoRecovered=32+(specialTeamsModifier(false,false,true,offensiveSelectedPlayers,defensiveSelectedPlayers,offensivePlayers,defensivePlayers)*-1);
			   if(whoRecovered<22)
			     {
			     whoRecovered=22;	
			     }
			   if(whoRecovered>42)
			     {
			     whoRecovered=42;	  
			     }
               playData=nextCard(playData,actionCards,random);
			   shuffledActionCards=playData.getShuffledActionCards();
			   cardData=playData.getCardData();
			   int recoverNumber=shuffledActionCards[cardData[0]][3];
			   if(recoverNumber>whoRecovered)
			     {
			     //Offense Recovers	
			     playResult[4]=whoBlockedOrRecoveredTheKick(false,true,false,false,offensiveSelectedPlayers,offensivePlayers,random);	
				 playData=nextCard(playData,actionCards,random);
				 shuffledActionCards=playData.getShuffledActionCards();
				 cardData=playData.getCardData();
				 int returnNumber=shuffledActionCards[cardData[0]][1];
				 playResult[6]=getSkill(true,0,offensivePlayers,78,returnNumber,true);
				 if(returnNumber==1)
				   {
				   playData=nextCard(playData,actionCards,random);
				   shuffledActionCards=playData.getShuffledActionCards();
				   cardData=playData.getCardData();
				   returnNumber=shuffledActionCards[cardData[0]][1];
				   int breakOut=getSkill(true,0,offensivePlayers,79,returnNumber,true);	
				   if(breakOut>0)
				     {		
				     playResult[6]=breakOut;	 
				     }
				   }  
				 if(playResult[6]!=0)
				   {
				   playResult[6]+=random.nextInt(11)-5;
				   }
				 if(ballOn+(playResult[38]*-1)-playResult[6]<0)
				   {
				   playResult[6]=ballOn+(playResult[38]*-1);	  
				   }
				 if(ballOn+(playResult[38]*-1)-playResult[6]>100)
				   {
				   playResult[6]=100-(ballOn+(playResult[38]*-1));	  
				   }				   
				 }
			   else
			     {
			     //Defense Recovers	
				 playResult[31]=1;	
				 playResult[4]=whoBlockedOrRecoveredTheKick(false,true,false,false,defensiveSelectedPlayers,defensivePlayers,random);	
			     playData=nextCard(playData,actionCards,random);
				 shuffledActionCards=playData.getShuffledActionCards();
				 cardData=playData.getCardData();
				 int returnNumber=shuffledActionCards[cardData[0]][1];
				 playResult[7]=getSkill(true,0,defensivePlayers,78,returnNumber,true);
				 if(returnNumber==1)
				   {
				   playData=nextCard(playData,actionCards,random);
				   shuffledActionCards=playData.getShuffledActionCards();
				   cardData=playData.getCardData();
				   returnNumber=shuffledActionCards[cardData[0]][1];
				   int breakOut=getSkill(true,0,offensivePlayers,79,returnNumber,true);	
				   if(breakOut>0)
				     {		
				     playResult[7]=breakOut;	 
				     }
				   }  
				 if(playResult[7]!=0)
				   {
				   playResult[7]+=random.nextInt(11)-5;
				   }	
				 if(ballOn+(playResult[38]*-1)+playResult[7]>100)
				   {
				   playResult[7]=100-(ballOn+(playResult[38]*-1));	  
				   }
				 if(ballOn+(playResult[38]*-1)+playResult[6]<0)
				   {
				   playResult[6]=100-(ballOn+(playResult[38]*-1));	  
				   }
				 }
			   }
		     }
		 }		  
	   if(offensivePlay[4]==28||offensivePlay[4]==31)
		 {
		 //Fake Kick	  
		 }		  
	   if(!offensivePlayFound&&fixtureNumber==1)
	     {
		 Routines.writeToLog(servletName,"["+fixtureNumber+"]Offensive Play Not Found="+offensivePlay[4]+" ",false,context);	
	     }
	   if(!defensivePlayFound&&fixtureNumber==1)
		 {
		 Routines.writeToLog(servletName,"["+fixtureNumber+"]Defensive Play Not Found="+defensivePlay[4],false,context);	
		 }	     
	   playData.setResults(playResult);
	   return playData;	
	   }

	private int whoToDoubleTeam(int defensiveTeam,
							    int offensivePlay,
							    int[] defensiveSelectedPlayers,
							    int[][] defensivePlayers,
							    Connection database,
							    boolean run,
							    Random random)
	    {
		int doubleTeamPosition=-1;
		int box1Position=0;
		int box2Position=0;
		if(run)
		  {
   	      if(offensivePlay==5||offensivePlay==7)
		    {
		    //Pitch or Option Left, if LDT is present always double him.	
		    if(defensiveSelectedPlayers[2]!=-1||defensiveSelectedPlayers[3]!=-1)
			  {
			  doubleTeamPosition=2;	
		  	  }		  
		    }
		  if(offensivePlay==6||offensivePlay==8)
		    {  
            //Pitch or Option Right, if RDT is present always double him.	
		    if(defensiveSelectedPlayers[6]!=-1||defensiveSelectedPlayers[7]!=-1)
			  {
			  doubleTeamPosition=6;	
			  }          
		    }
		  if(doubleTeamPosition==-1)
		    {
		    switch(offensivePlay)
		     {
		     case 1:
			    //Inside Left
			    box1Position=2;
			    box2Position=4;
			    break;
		     case 2:
			    //Inside Right
			    box1Position=4;
			    box2Position=6;
			    break;	
		     case 3:
			    //Sweep Left
			    box1Position=0;
			    box2Position=2;
			    break;
		     case 4:
			    //Sweep Right	
			    box1Position=6;
			    box2Position=8;
			    break;
		     case 5:
			    //Pitch Left
			    box1Position=0;
			    box2Position=4;			  
			    break;
		     case 6:
			    //Pitch Right
			    box1Position=4;
			    box2Position=8;			  
			    break;
		     case 7:
			    //Option Left
			    box1Position=0;
			    box2Position=4;				  
			    break;
		     case 8:
			    //Option Right
			    box1Position=4;
			    box2Position=8;				  
			    break;			  
		     default:
			    Routines.writeToLog(servletName,"Possible Positions for Double Team Calculation Not Found: " + offensivePlay,false,context);
		     }
             boolean box1=false;
		     boolean box2=false;
		     if(defensiveSelectedPlayers[box1Position]!=-1||defensiveSelectedPlayers[box1Position+1]!=-1)
			   {
			   box1=true;	
			   }
		     if(defensiveSelectedPlayers[box2Position]!=-1||defensiveSelectedPlayers[box2Position+1]!=-1)
			   {
			   box2=true;	
			   }			      
		     if(!box1&&!box2)
			   {
			   doubleTeamPosition=box1Position;		    
			   }
		     if(box1&&!box2)
			   {
			   doubleTeamPosition=box1Position;
			   }
		     if(!box1&&box2)
			   {
			   doubleTeamPosition=box2Position;	  
			   }
		     if(box1&&box2)
			   {
			   int blockingValue1=0;
			   int blockingValue2=0;					 
			   if(defensiveSelectedPlayers[box1Position]!=-1)
			     {
			     blockingValue1+=Routines.getTotalSkills(defensiveTeam,defensivePlayers[defensiveSelectedPlayers[box1Position]][1],database,context);
			     }
			   if(defensiveSelectedPlayers[box1Position+1]!=-1)
			     {					    	
			     blockingValue1+=Routines.getTotalSkills(defensiveTeam,defensivePlayers[defensiveSelectedPlayers[box1Position+1]][1],database,context);
			     }
			   if(defensiveSelectedPlayers[box2Position]!=-1)
			     {
			     blockingValue2+=Routines.getTotalSkills(defensiveTeam,defensivePlayers[defensiveSelectedPlayers[box2Position]][1],database,context);
			     }
			   if(defensiveSelectedPlayers[box2Position+1]!=-1)
			     {					    	
			     blockingValue2+=Routines.getTotalSkills(defensiveTeam,defensivePlayers[defensiveSelectedPlayers[box2Position+1]][1],database,context);
			     }
			   if(blockingValue1>blockingValue2)
			     { 
			     doubleTeamPosition=box1Position;	 					   	
			     }
			   if(blockingValue1<blockingValue2)
			     { 
			     doubleTeamPosition=box2Position;	 					   	
			     }
			   if(blockingValue1==blockingValue2)
			     { 
			     int randomPick=random.nextInt(2);
			     if(randomPick==0)
				   {	
				   doubleTeamPosition=box1Position;
				   }
			     else
				   {
				   doubleTeamPosition=box2Position;	  	 					   	
				   }
			     }					   					   
			   }
		     if(doubleTeamPosition==-1)
			   {  
			   Routines.writeToLog(servletName,"Double Team Calculation has not worked : " + offensivePlay,false,context);	
			   }
		    }
		  }
		else
		  { 
		  int passRushValue=-1;
		  for(int currentDL=0;currentDL<10;currentDL++)
		     {
		     if(defensiveSelectedPlayers[currentDL]!=-1)
		       {	
		       int passRushSkill=getBlockingSkill(defensivePlayers[defensiveSelectedPlayers[currentDL]][1],false);
			   int tempPassRushValue=getSkill(false,defensiveSelectedPlayers[currentDL],defensivePlayers,passRushSkill,1,false);
			   if(tempPassRushValue>passRushValue)
			     {
			     passRushValue=tempPassRushValue;
			     doubleTeamPosition=currentDL;	  		   
			     }
		       }  
			 }
		   }
		return doubleTeamPosition;   		     
		}
	
	private int getPrimaryDoubleTeamer(int positionToBlock)
		{
		int positionNumber=0;
		switch(positionToBlock)
		   {
		   case 0:
		      //LDE
		      positionNumber=2;
	          break;		
		   case 2:
			  //LDT
			  positionNumber=3;
			  break;		
		   case 4:
			  //NT
			  positionNumber=4;
			  break;		
		   case 6:
			  //RDT
			  positionNumber=5;
			  break;		
		   case 8:
			  //RDE
			  positionNumber=6;
			  break;			          	  
		   default:
		      Routines.writeToLog(servletName,"Primary Double Teamer Not Found: " + positionToBlock,false,context);
		   }
		return positionNumber;	
		}		

	private int getSecondaryDoubleTeamer(int primaryDoubleTeamer,
	                                     int[] offensiveSelectedPlayers,
	                                     int[][] offensivePlayers,
	                                     Random random)
		{
		int positionNumber=-1;
		int positionNumber1=0;
		int positionNumber2=0;
		switch(primaryDoubleTeamer)
		   {
		   case 2:
			  //LT
			  positionNumber1=1;
              positionNumber2=3;
			  break;		
		   case 3:
			  //LG
			  positionNumber1=2;
			  positionNumber2=4;
			  break;		
		   case 4:
			  //C
			  positionNumber1=3;
			  positionNumber2=5;
			  break;		
		   case 5:
			  //RG
			  positionNumber1=4;
			  positionNumber2=6;
			  break;		
		   case 6:
			  //RT
			  positionNumber1=5;
			  positionNumber2=7;
			  break;			          	  
		   default:
			  Routines.writeToLog(servletName,"Secondary Double Teamer Not Found: " + primaryDoubleTeamer,false,context);
		   }
		 int blockingSkill1=0;
		 int blockingSkill2=0;
		 if(offensiveSelectedPlayers[positionNumber1]!=-1)
		   {
		   blockingSkill1=getBlockingSkill(offensivePlayers[offensiveSelectedPlayers[positionNumber1]][1],true);
		   }
		 else
		   {
	       blockingSkill1=getBlockingSkill(offensivePlayers[offensiveSelectedPlayers[0]][1],true);	
	       }
		 if(offensiveSelectedPlayers[positionNumber2]!=-1)
		   {
		   blockingSkill2=getBlockingSkill(offensivePlayers[offensiveSelectedPlayers[positionNumber2]][1],true);
		   }
		 else
		   {
		   blockingSkill2=getBlockingSkill(offensivePlayers[offensiveSelectedPlayers[8]][1],true);	
		   }
		 if(blockingSkill1>blockingSkill2)
		   {
		   positionNumber=positionNumber2;	  
		   }
		 if(blockingSkill1<blockingSkill2)
		   {
		   positionNumber=positionNumber1;	  
		   }			    
		 if(blockingSkill1==blockingSkill2)
		   { 
		   int randomPick=random.nextInt(2);
		   if(randomPick==0)
			 {	
			 positionNumber=positionNumber1;
			 }
		   else
			 {
			 positionNumber=positionNumber2;	  	 					   	
			 }
		   }	 
	    if(positionNumber==-1)
	      {  
	      Routines.writeToLog(servletName,"Secondary Double Team Calculation has not worked : " + primaryDoubleTeamer,false,context);	
	      }
		return positionNumber;	
		}		
	
	private boolean isDoubleTeamerReferenced(boolean doubleTeam,
	                                         int doubleTeamer,
	                                         int currentBlock)
	   {
	   boolean doubleTeamerReferenced=false;	
	   if(doubleTeam&&doubleTeamer==currentBlock)
		 {
		 doubleTeamerReferenced=true;	
		 }
	   return doubleTeamerReferenced;	 
	   }
	   
	public boolean stillActive()
	   {
	   return active;
	   }	  
   }	  	

class playObject 
   {
   private int[][] shuffledActionCards;
   private int[] playResults;
   private int[] cardData;
   
   public void setActionCards(int[][] shuffledActionCards)
	 {
	 this.shuffledActionCards=shuffledActionCards;
	 }   	
   public void setResults(int[] playResults)
     {
     this.playResults=playResults;			 
     }
   public void setCardData(int[] cardData)
     {
     this.cardData=cardData;	  
     }
   public int[][] getShuffledActionCards()
     {
     return shuffledActionCards;	  
     }
   public int[] getPlayResults()
     {
     return playResults;	
     }
   public int[] getCardData()
     {
     return cardData;	  
     }
   }	
//               StandingsEngine standingsEngine1 = new StandingsEngine(leagueNumber,
//                                                                      season,
//                                                                      week,
//                                                                      nextSeason,
//                                                                      nextWeek,
//                                                                      game,
//                                                                      true,
//                                                                      database);
//               StandingsEngine standingsEngine2 = new StandingsEngine(leagueNumber,
//                                                                      season,
//                                                                      week,
//                                                                      nextSeason,
//                                                                      nextWeek,
//                                                                      game,
//                                                                      false,
//                                                                      database);
//               }
//         StandingsEngine UpdatePositions = new StandingsEngine(leagueNumber,
//                                                               nextSeason,
//                                                               nextWeek,
//                                                               database);
//         }
//      catch( SQLException error )
//         {
//         System.out.println("Database error in GameEngine.Constructor(int,Connection) getting Fixtures: " + error.getMessage() );
//         }
//      updateLeague(leagueNumber,leagueType,nextSeason,nextWeek,database);
//      }
//
//   private void playGame(Game game,
//                         Connection database)
//      {
////      String awayTeamName = game.getAwayTeamName();
////      String homeTeamName = game.getHomeTeamName();
//      Random randomScore  = new Random((long)game.getFixtureNumber());
//      Random randomScore2 = new Random(((long)game.getFixtureNumber()) + 1);
//      int ignoreScore     = (int)(randomScore.nextDouble() * 50);
//      int ignoreScore2    = (int)(randomScore2.nextDouble() * 50);
//      int awayTeamScore   = (int)(randomScore.nextDouble() * 50);
//      int homeTeamScore   = (int)(randomScore2.nextDouble() * 50);
//      try
//        {
//        Statement sql = database.createStatement();
//        ResultSet rs = sql.executeQuery( "UPDATE fixtures " +
//                                         "SET HomeTeamScore = " +
//                                         homeTeamScore +
//                                         ",AwayTeamScore = " +
//                                         awayTeamScore +
//                                         " WHERE FixtureNumber = " +
//                                         game.getFixtureNumber());
//        }
//     catch( SQLException e )
//        {
//        System.out.println("Database error updating fixtures: " +e.getMessage() );
//        }
//     game.setHomeTeamScore(homeTeamScore);
//     game.setAwayTeamScore(awayTeamScore);
//     }
//
//   private void updateLeague(int leagueNumber,
//                             int leagueType,
//                             int season,
//                             int week,
//                             Connection database)
//     {
//     try
//       {
//       Statement sql = database.createStatement();
//       ResultSet rs = sql.executeQuery( "UPDATE leagues " +
//                                        "SET Season = " +
//                                        season +
//                                        ",Week = " +
//                                        week +
//                                        " WHERE LeagueNumber = " +
//                                        leagueNumber);
//       }
//    catch( SQLException e )
//       {
//       System.out.println("Database error updating Leagues: " +e.getMessage() );
//       }
//    }
//}