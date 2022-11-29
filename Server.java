package task;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;

import com.mysql.cj.protocol.Resultset;
import com.mysql.cj.xdevapi.Result;

import java.sql.*;

public class Server {
	static int count = 1;
	static HashSet<Handler> users = new HashSet<>();
	static HashSet<Group> Groups = new HashSet<>();
	static HashSet<String> GroupNames = new HashSet<>();

	public static void main(String[] args) throws IOException, ClassNotFoundException, SQLException {

		Class.forName("com.mysql.cj.jdbc.Driver");
		Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/demo", "root", "123");
		ServerSocket ss = new ServerSocket(9995);
		while (true) {
			Socket s = ss.accept();
			System.out.println("Connection has been established with new socket" + s);

			Handler x = new Handler(s, con);

			Thread t = new Thread(x);
			users.add(x);
			t.start();
			count++;
		}

	}
}

class Handler implements Runnable {
	String name;
	Socket s;
	DataOutputStream os;
	DataInputStream is;
	Connection conn;

	public Handler(Socket s, Connection conn) throws IOException {
		this.conn = conn;
		this.s = s;
		this.os = new DataOutputStream(s.getOutputStream());
		this.is = new DataInputStream(s.getInputStream());
	}

	String endClient = "";
	int Current_client_id = 0;
	int End_client_id = 0;
	int Group_id = 0;
	String msg = "";
	String GroupName = "";
	String client = "";
	String client_to_add = "";
	String Pending_Group = "";
	String End_client_name = "";
	Group g;

	public void run() {

		try {
			while (true) {
				String UserInput = is.readUTF();
				Statement statement = conn.createStatement();

				if (UserInput.contains("CheckClient:")) {
					StringTokenizer st = new StringTokenizer(UserInput, ":");
					while (st.hasMoreElements()) {
						UserInput = st.nextToken();
					}
					String query = "Select * from users where userName='" + UserInput + "'";
					ResultSet rs = statement.executeQuery(query);
					if (rs.next() == false) {
						os.writeUTF("User not found");
						continue;
					}
					Current_client_id = rs.getInt("userId");
					this.name = UserInput;
					os.writeUTF("Welcome " + name);
				}

				if (UserInput.contains("Client__Name")) {
					StringTokenizer st = new StringTokenizer(UserInput, ":");
					while (st.hasMoreElements()) {
						UserInput = st.nextToken();
					}
					String query = "Select * from users where userName='" + UserInput + "'";
					ResultSet rs = statement.executeQuery(query);
					if (rs.next()) {
						os.writeUTF("UserName already exists. Try a different name.");
						continue;
					} else {
						this.name = UserInput;
						String query2 = "Insert into users (userName) values ('" + UserInput + "')";
						statement.executeUpdate(query2);
						ResultSet res = statement.executeQuery(query);
						while (res.next()) {
							Current_client_id = res.getInt("userId");
						}
						os.writeUTF("Welcome" + name);
					}
				}

				if (UserInput.contains("ack#")) {
					StringTokenizer st = new StringTokenizer(UserInput, "#");
					while (st.hasMoreElements()) {
						UserInput = st.nextToken();
					}
					for (Handler h : Server.users) {
						if (h.name != null && h.name.equals(UserInput)) {
							h.os.writeUTF("message sent succesfully to" + name);
						}
					}
				}

				if (UserInput.equals("#GetUsers")) {
					ResultSet rs = statement.executeQuery("Select userName from users");
					int counter = 0;
					while (rs.next()) {
						if (rs.getString("userName").equals(name))
							continue;
						os.writeUTF(rs.getString("userName"));
						counter++;
					}
					if (counter < 1)
						os.writeUTF("No Clients Available");
				}

				if (UserInput.contains("clientName :")) {
					StringTokenizer st = new StringTokenizer(UserInput, ":");
					while (st.hasMoreElements()) {
						endClient = st.nextToken();
					}
					String query = "SELECT * FROM users WHERE userName ='" + endClient + "'";
					ResultSet rs = statement.executeQuery(query);
					if (rs.next() == false) {
						os.writeUTF("No such Client");
					} else {
						End_client_id = rs.getInt("userId");
						End_client_name = rs.getString("userName");
						
						Statement stmt = conn.createStatement();
						stmt.executeUpdate("UPDATE users SET connected_to = '"+ End_client_name+"' WHERE userName = '"+name+"'");
//						connections ct = new connections(name, End_client_name);
//						Server.connected_users.add(ct);
						
						ResultSet res = statement.executeQuery("SELECT * FROM chats WHERE from_id='" + End_client_id
								+ "' AND to_id='" + Current_client_id + "'UNION SELECT * FROM chats WHERE from_id='"
								+ Current_client_id + "' AND to_id='" + End_client_id + "' ORDER BY s_no ");
						
						
						boolean r = false;
						while (res.next()) {
							String temp = res.getString("text");
							int from_id = res.getInt("from_id");
							int read = res.getInt("check_read");
							String time = res.getString("timestamp");
							if (read == 0 && r == false && Current_client_id != from_id) {
								os.writeUTF("UNREAD MESSAGES:");
								Statement stmt1 = conn.createStatement();
								System.out.println(from_id +" "+ Current_client_id);
								stmt1.executeUpdate("UPDATE chats SET check_read = 1 WHERE check_read = 0 And  from_id = "+End_client_id +" And to_id = "+Current_client_id);
								r = true;
							}
							String user_name = "";
							if (from_id == End_client_id) {
								user_name = End_client_name;
							} else {
								user_name = name;
							}
							os.writeUTF(user_name + ":" + temp+ "  " + time);
						}
						os.writeUTF("chat with " + endClient);

					}

				}

				if (UserInput.contains("message :")) {
					
					String  time = ""+ new Timestamp(System.currentTimeMillis());
					StringTokenizer st = new StringTokenizer(UserInput, ":");
					while (st.hasMoreElements()) {
						msg = st.nextToken();
					}
					os.writeUTF("Message received by Server");
					String query = "Insert into chats (text,from_id,to_id, timestamp) values (' 	" + msg + "',"
							+ Current_client_id + "," + End_client_id + ", '"+ time+"')";
					statement.executeUpdate(query);
					boolean logged_in = false;
					
					ResultSet res = statement.executeQuery("Select * from users where userName ='"+End_client_name+"'");
					String con_name = "";
					while(res.next()) {
						 con_name = res.getString("connected_to");
					}
					if(con_name != null){
						if(con_name.equals(this.name)) {
							logged_in = true;
						}
					}
					if(logged_in) {
						ResultSet rs = statement.executeQuery("SELECT * FROM chats ORDER BY s_no DESC LIMIT 1");
						String time1="";
						while(rs.next()) {
							 time1 = res.getString("timestamp");
						}
					for (Handler h : Server.users) {
						if (h.name != null && h.name.equals(endClient)) {
							h.os.writeUTF(name + " : " + msg+ " "+time1);
						}
					}
					}
					else if (logged_in == false) {
						int s_no = 0;
						ResultSet rs = statement.executeQuery("SELECT * FROM chats ORDER BY s_no DESC LIMIT 1");
						while (rs.next()) {
							s_no = rs.getInt("s_no");
						}
						statement.executeUpdate("UPDATE chats SET check_read = 0 WHERE s_no =" + s_no);
					}
				}
				
				
				if(UserInput.contains("#disconnected:")) {
					StringTokenizer st = new StringTokenizer(UserInput, ":");
					String temp_name = "";
					while (st.hasMoreElements()) {
						temp_name = st.nextToken();
					}
					statement.executeUpdate("UPDATE users SET connected_to = null WHERE userName = '"+temp_name+"'");
					
				}

				if(UserInput.contains("#disconnectedGrp:")) {
					StringTokenizer st = new StringTokenizer(UserInput, ":");
					String temp_name = "";
					while (st.hasMoreElements()) {
						temp_name = st.nextToken();
					}
					statement.executeUpdate("UPDATE users SET connected_group = null WHERE userName = '"+temp_name+"'");
					
				}
				
				
				if (UserInput.equals("#showGroups")) {
					String query = "SELECT * FROM users WHERE userName ='" + name + "'";
					ResultSet rs = statement.executeQuery(query);
					int user_id = 0;
					while (rs.next()) {
						user_id = rs.getInt("userId");
					}
					Server.GroupNames.clear();
					ResultSet rs2 = statement.executeQuery(
							"Select groupnames.Group_name from groupnames join map_groupid on map_groupid.group_id = groupnames.group_id where map_groupid.user_id = "
									+ user_id);
					while (rs2.next()) {
						String st = rs2.getString("Group_name");
						Server.GroupNames.add(st);
						os.writeUTF(st);
						g = new Group(st, os);
						Server.Groups.add(g);
					}

				}

				if (UserInput.contains("GroupName :")) {
					StringTokenizer st = new StringTokenizer(UserInput, ":");
					while (st.hasMoreElements()) {
						GroupName = st.nextToken();
					}
					if (!Server.GroupNames.contains(GroupName)) {
						os.writeUTF("No such Group");
					} else {
						ResultSet rs = statement
								.executeQuery("Select * from groupnames where Group_name = '" + GroupName + "'");
						while (rs.next()) {
							Group_id = rs.getInt("group_id");
						} 
						statement.executeUpdate("Update users Set connected_group = "+Group_id+ " where userId = "+Current_client_id);
						ResultSet res = statement
								.executeQuery("Select * from group_chats where to_group_id = " + Group_id);
						Statement st2 = conn.createStatement();
						Statement st3 = conn.createStatement();
						
						
						while (res.next()) {
							
							int msg_id = res.getInt("s_no");
							int from_id = res.getInt("from_id"); 
							System.out.println(from_id);
							ResultSet rs1 = st2.executeQuery("Select * from users where userId ="+from_id);
							String username = "";
							
							while(rs1.next()) {
								username = rs1.getString("userName");
							}
							
							ResultSet rs2 = st3.executeQuery("Select * from map_msgid where msg_id = "+msg_id);
							if(rs2.next()) {
								os.writeUTF("Unread Messages:");
								st3.executeUpdate("Delete from map_msgid where user_id = "+Current_client_id);
							}
							os.writeUTF(username+" : " +res.getString("texts"));
						}
						os.writeUTF("Send messages to " + GroupName);
					}
				}

				if (UserInput.contains("#CreateGroup :")) {
					StringTokenizer st = new StringTokenizer(UserInput, ":");
					while (st.hasMoreElements()) {
						GroupName = st.nextToken();
					}
					ResultSet rs = statement
							.executeQuery("Select * from groupnames where Group_name = '" + GroupName + "'");
					if (rs.next()) {
						os.writeUTF("Group already exists");
					} else {
						statement.executeUpdate("Insert into groupnames(Group_name) values ('" + GroupName + "')");
						String query = "Select * from groupnames where Group_name = '" + GroupName + "'";
						ResultSet res = statement.executeQuery(query);
						int id = 0;
						while (res.next()) {
							id = res.getInt("group_id");
						}
						statement.executeUpdate("Insert into map_groupid (group_id,user_id) values (" + id + ","
								+ Current_client_id + ")");
						Group g = new Group(GroupName, os);
						for (Handler h : Server.users) {
							g.clients.add(this);
							g.clientNames.add(this.name);
							if (h.name!=null&& h.name.equals(name))
								continue;
							os.writeUTF(h.name);
						}
						Server.Groups.add(g);
					}
				}

				if (UserInput.contains("#AddUser :")) {
					StringTokenizer st = new StringTokenizer(UserInput, "#");
					String temp = "", target_client = "";
					temp = st.nextToken();
					target_client = st.nextToken();

					String query1 = "Select * from groupnames where Group_name = '" + GroupName + "'";
					ResultSet rs = statement.executeQuery(query1);
					int g_id = 0;
					while (rs.next()) {
						g_id = rs.getInt("group_id");
					}
					String query = "SELECT * FROM users WHERE userName ='" + target_client + "'";
					ResultSet res = statement.executeQuery(query);
					if (res.next()) {
						int id = 0;
						id = res.getInt("userId");
						statement.executeUpdate(
								"Insert into map_groupid (group_id,user_id) values (" + g_id + "," + id + ")");
						os.writeUTF("You added " + target_client);
						for (Group g : Server.Groups) {
							if (g.name.equals(GroupName)) {
								for (Handler h : Server.users) {
									if (h.name.equals(target_client)) {
										g.clients.add(h);
										g.clientNames.add(target_client);
										h.os.writeUTF(name + " added you in the Group " + "[" + g.name + "]");
									}
								}
							}
						}
					} else {
						os.writeUTF("No such Client");
					}
				}

				if (UserInput.contains("GrpMessage :") && !UserInput.contains("#Add")) {
					os.writeUTF("Message received by Server");
					StringTokenizer st = new StringTokenizer(UserInput, ":");
					while (st.hasMoreElements()) {
						msg = st.nextToken();
					}

					String query = "Insert into group_chats (texts,from_id,to_group_id) values ('" + msg
							+ "'," + Current_client_id + "," + Group_id + ")";
					statement.executeUpdate(query);
					
					ResultSet msgSet = statement.executeQuery("SELECT * FROM group_chats ORDER BY s_no DESC LIMIT 1");
					int msgId = 0;
					while(msgSet.next()) {
						msgId = msgSet.getInt("s_no");
					}
					Statement st1 = conn.createStatement();
					ResultSet rs =  statement.executeQuery("Select userId , userName , connected_group from users join map_groupid on users.userId = map_groupid.user_id where map_groupid.group_id = "+Group_id);
					while(rs.next()) {
						int tempId = rs.getInt("connected_group");
						if(tempId == Group_id) {
							for (Handler h : Server.users) {
								if (h.name!=null&&h.name.equals(rs.getString("userName")) && !h.name.equals(name)) {
									h.os.writeUTF(name + " : [Group " + g.name + "]-->  " + msg);
								}
							}
						}else if(tempId != Group_id) {
							st1.executeUpdate("Insert into map_msgid (msg_id , user_id) values (" + msgId + " , " + rs.getInt("userId") + ")");
						}
					}
//					for (Group g : Server.Groups) {
//						if (g.name.equals(GroupName)) {
//							for (Handler h : g.clients) {
//								if (h.name.equals(name))
//									continue;
//
//								h.os.writeUTF(name + " : [Group " + g.name + "]-->  " + msg);
//							}
//						}
//					}
				}



				if (UserInput.contains("#logout")) {
					Server.users.remove(this);
					is.close();
					os.close();
					break;
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

}

class Group {
	String name;
	DataOutputStream os;
	HashSet<Handler> clients = new HashSet<Handler>();
	HashSet<String> clientNames = new HashSet<>();

	public Group(String name, DataOutputStream os) throws IOException {
		this.name = name;
		this.os = os;

	}

}

