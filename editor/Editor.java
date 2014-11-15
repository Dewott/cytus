package cytus.editor;

/*
 * To compile:
 *  javac -encoding UTF-8 Editor.java
 */
import cytus.*;
import java.io.*;
import java.util.*;
import java.text.*;
import java.awt.*;
import java.awt.event.*;
import javax.media.*;
import javax.swing.*;
import javax.swing.undo.*;
import javax.swing.event.*;
import javax.swing.table.*;

public class Editor extends JFrame implements ActionListener,
		ListSelectionListener, MouseListener, StateEditable, PopupMenuListener {
	SimplePatternPlayer spp = null;
	PreviewWindow pwindow = null;
	UndoManager manager = null;
	JMenu m_edit = null;
	JMenu p_addtogroup = null, m_dgroups = null;

	Pattern pdata = null;
	Timing timing = null;
	JTable table = null;
	PatternDataTable model = null;
	TableRowSorter<PatternDataTable> sorter = null;
	DataFilter filter = null;

	LinkedList<Pattern.Note> selection = new LinkedList<Pattern.Note>();
	LinkedList<NoteGroup> groups = new LinkedList<NoteGroup>();
	HashMap<String, NoteGroup> gnames = new HashMap<String, NoteGroup>();

	public Editor() throws Exception {
		super("����༭��");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(600, 400);
		setVisible(true);

		JMenuBar root = new JMenuBar();

		// Menu File
		JMenu m_file = new JMenu("�ļ�");
		JMenuItem m_fopen = new JMenuItem("��");
		m_fopen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
				InputEvent.CTRL_DOWN_MASK));
		JMenuItem m_fsave = new JMenuItem("����");
		m_fsave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
				InputEvent.CTRL_DOWN_MASK));
		JMenuItem m_fsaveas = new JMenuItem("���Ϊ");
		m_file.add(m_fopen);
		m_file.add(m_fsave);
		m_file.add(m_fsaveas);
		m_fopen.addActionListener(this);
		m_fsave.addActionListener(this);
		m_fsaveas.addActionListener(this);
		root.add(m_file);

		// Menu Edit
		m_edit = new JMenu("�༭");
		JMenuItem m_eselall = new JMenuItem("ȫѡ");
		m_eselall.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A,
				InputEvent.CTRL_DOWN_MASK));
		JMenuItem m_eclrsel = new JMenuItem("ȡ��ѡ��");
		m_eclrsel.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D,
				InputEvent.CTRL_DOWN_MASK));
		JMenuItem m_eundo = new JMenuItem("����");
		m_eundo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
				InputEvent.CTRL_DOWN_MASK));

		JMenuItem p_newnote = new JMenuItem("���Note");
		JMenuItem p_jump = new JMenuItem("��ת��ָ��ʱ��");
		JMenuItem p_newlink = new JMenuItem("�½�Link");
		JMenuItem p_addtolink = new JMenuItem("��ӵ�ָ����Link");
		JMenuItem p_rmfromlink = new JMenuItem("��Link���Ƴ�");
		JMenuItem p_unlink = new JMenuItem("�⿪Link");
		JMenuItem p_split = new JMenuItem("���Link");

		p_addtogroup = new JMenu("���������");
		JMenuItem p_newgroup = new JMenuItem("�·���");
		p_addtogroup.add(p_newgroup);
		p_addtogroup.addSeparator();
		for (NoteGroup g : groups) {
			JMenuItem p_group = new JMenuItem(g.name);
			p_group.addActionListener(this);
			p_addtogroup.add(p_group);
		}

		JMenuItem p_mirrorcopy = new JMenuItem("ˮƽ������");
		JMenuItem p_timeshift = new JMenuItem("�ƶ�ʱ��");
		JMenuItem p_del = new JMenuItem("ɾ��");
		p_del.setAccelerator(KeyStroke.getKeyStroke(127, 0)); // Del

		m_edit.add(m_eselall);
		m_edit.add(m_eclrsel);
		m_edit.add(m_eundo);
		m_edit.addSeparator();
		m_edit.add(p_newnote);
		m_edit.add(p_jump);
		m_edit.add(p_addtogroup);
		m_edit.add(new JSeparator());
		m_edit.add(p_newlink);
		m_edit.add(p_addtolink);
		m_edit.add(p_rmfromlink);
		m_edit.add(p_unlink);
		m_edit.add(p_split);
		m_edit.add(p_mirrorcopy);
		m_edit.add(p_timeshift);
		m_edit.add(p_del);

		m_eselall.addActionListener(this);
		m_eclrsel.addActionListener(this);
		m_eundo.addActionListener(this);
		p_newnote.addActionListener(this);
		p_jump.addActionListener(this);
		p_newgroup.addActionListener(this);
		p_newlink.addActionListener(this);
		p_addtolink.addActionListener(this);
		p_rmfromlink.addActionListener(this);
		p_unlink.addActionListener(this);
		p_split.addActionListener(this);
		p_mirrorcopy.addActionListener(this);
		p_timeshift.addActionListener(this);
		p_del.addActionListener(this);
		m_edit.getPopupMenu().addPopupMenuListener(this);
		root.add(m_edit);

		// Menu Settings
		JMenu m_settings = new JMenu("����");
		JMenuItem m_sglobal = new JMenuItem("ȫ������");
		JMenu m_sgrid = new JMenu("��������");
		JCheckBoxMenuItem m_sgenable = new JCheckBoxMenuItem("��ʾ����", true);
		ButtonGroup group1 = new ButtonGroup();
		JRadioButtonMenuItem m_sgx10 = new JRadioButtonMenuItem("x���:1/10",
				true);
		JRadioButtonMenuItem m_sgx20 = new JRadioButtonMenuItem("x���:1/20");
		group1.add(m_sgx10);
		group1.add(m_sgx20);
		ButtonGroup group2 = new ButtonGroup();
		JRadioButtonMenuItem m_sgy4 = new JRadioButtonMenuItem("y���:1/4");
		JRadioButtonMenuItem m_sgy8 = new JRadioButtonMenuItem("y���:1/8", true);
		JRadioButtonMenuItem m_sgy16 = new JRadioButtonMenuItem("y���:1/16");
		group2.add(m_sgy4);
		group2.add(m_sgy8);
		group2.add(m_sgy16);
		m_sgrid.add(m_sgenable);
		m_sgrid.addSeparator();
		m_sgrid.add(m_sgx10);
		m_sgrid.add(m_sgx20);
		m_sgrid.addSeparator();
		m_sgrid.add(m_sgy4);
		m_sgrid.add(m_sgy8);
		m_sgrid.add(m_sgy16);

		m_settings.add(m_sglobal);
		m_settings.add(m_sgrid);
		m_sglobal.addActionListener(this);
		m_sgenable.addActionListener(this);
		m_sgx10.addActionListener(this);
		m_sgx20.addActionListener(this);
		m_sgy4.addActionListener(this);
		m_sgy8.addActionListener(this);
		m_sgy16.addActionListener(this);

		root.add(m_settings);

		// Menu Data
		JMenu m_data = new JMenu("����");
		JMenuItem m_dcheck = new JMenuItem("���д�����");
		ButtonGroup group3 = new ButtonGroup();
		JRadioButtonMenuItem m_dall = new JRadioButtonMenuItem("��ʾ����Note", true);
		JRadioButtonMenuItem m_dcircles = new JRadioButtonMenuItem("��ʾ����Circle");
		JRadioButtonMenuItem m_dholds = new JRadioButtonMenuItem("��ʾ����Hold");
		JRadioButtonMenuItem m_dlinks = new JRadioButtonMenuItem("��ʾ����Link");
		m_dgroups = new JMenu("��ʾ����");
		group3.add(m_dall);
		group3.add(m_dcircles);
		group3.add(m_dholds);
		group3.add(m_dlinks);
		m_data.add(m_dcheck);
		m_data.addSeparator();
		m_data.add(m_dall);
		m_data.add(m_dcircles);
		m_data.add(m_dholds);
		m_data.add(m_dlinks);
		m_data.add(m_dgroups);
		m_dcheck.addActionListener(this);
		m_dall.addActionListener(this);
		m_dcircles.addActionListener(this);
		m_dholds.addActionListener(this);
		m_dlinks.addActionListener(this);
		root.add(m_data);

		add(root, "North");

		String songtitle = JOptionPane.showInputDialog(this, "Song title");
		BufferedReader in = new BufferedReader(new FileReader("assets/songs/"
				+ songtitle + "/" + songtitle + ".hard.txt"));
		if (in.readLine().equals("VERSION 2"))
			pdata = PatternReader2.read(in);
		else
			pdata = PatternReader1.read(in);

		pdata.offset = pdata.notes.get(0).time;
		timing = new Timing(pdata.beat, pdata.offset);
		manager = new UndoManager();

		model = new PatternDataTable();
		table = new JTable(model);
		table.setFocusable(true);
		table.getSelectionModel().addListSelectionListener(this);
		table.addMouseListener(this);

		filter = new DataFilter();
		sorter = new TableRowSorter<PatternDataTable>(model);
		sorter.setRowFilter(filter);
		for (int i = 0; i < 5; i++)
			sorter.setSortable(i, false);
		table.setRowSorter(sorter);

		JScrollPane scroll = new JScrollPane(table);
		add(scroll, "Center");
		validate();

		Player player = Manager.createRealizedPlayer(new MediaLocator(new File(
				"temp.wav").toURI().toURL()));
		spp = new SimplePatternPlayer(pdata, player);
		spp.selection = selection; // reference

		pwindow = new PreviewWindow();
	}

	public void actionPerformed(ActionEvent e) {
		System.out.println(e.getActionCommand());
		StateEdit edit = new StateEdit(this, e.getActionCommand());
		boolean modified = false;
		switch (e.getActionCommand()) {
		case "��":
			break;
		case "����":
			try {
				PrintWriter out = new PrintWriter(new FileOutputStream(
						"out.txt"));
				new PatternWriter(pdata, out);
			} catch (Exception e1) {
			}
			break;
		case "ȫѡ":
			selection.addAll(pdata.notes);
			table.selectAll();
			break;
		case "ȡ��ѡ��":
			selection.clear();
			table.clearSelection();
			break;
		case "����":
			undo();
			break;
		case "ȫ������":
			break;
		case "��ʾ����":
			pwindow.showgrid = !pwindow.showgrid;
			break;
		case "x���:1/10":
			pwindow.xdiv = 10;
			break;
		case "x���:1/20":
			pwindow.xdiv = 20;
			break;
		case "y���:1/4":
			pwindow.ydiv = 4;
			break;
		case "y���:1/8":
			pwindow.ydiv = 8;
			break;
		case "y���:1/16":
			pwindow.ydiv = 16;
			break;
		case "��ʾ����Note":
			filter.mode = 0;
			sorter.sort();
			break;
		case "��ʾ����Circle":
			filter.mode = 1;
			sorter.sort();
			break;
		case "��ʾ����Hold":
			filter.mode = 2;
			sorter.sort();
			break;
		case "��ʾ����Link":
			filter.mode = 3;
			sorter.sort();
			break;
		case "��ת��ָ��ʱ��":
			if (selection.size() == 1)
				spp.player.setMediaTime(new Time(selection.getFirst().time));
			break;
		case "���Note": {
			double x = Double.parseDouble(JOptionPane.showInputDialog(this,
					"x����"));
			double time = Double.parseDouble(JOptionPane.showInputDialog(this,
					"ʱ��"));
			pdata.notes.add(pdata.new Note(0, time, x, 0)); // Auto ID
			modified = true;
			break;
		}
		case "�½�Link": {
			if (selection.size() < 2)
				break;
			boolean check = true;
			for (Pattern.Note note : selection)
				if ((note.holdtime != 0) || (note.linkref != -1)) {
					check = false;
					break;
				}
			if (check) {
				String mode = (String) JOptionPane.showInputDialog(this,
						"���ӷ�ʽ", "�½�Link", JOptionPane.QUESTION_MESSAGE, null,
						new String[] { "��ͨ", "����" }, "��ͨ");
				Pattern.Link link = pdata.new Link(pdata.links.size());
				if (mode.equals("��ͨ")) {
					for (Pattern.Note note : selection)
						link.add(note);
				} else {
					int i = 0;
					for (i = 0; i < selection.size(); i++)
						if (timing.getBeatType(selection.get(i).time,
								pwindow.ydiv) == -1)
							link.add(selection.get(i));

					Collections.sort(selection);
					double t = timing.getNearestBeat(selection.getFirst().time,
							pwindow.ydiv);
					while (t < selection.getFirst().time)
						t += pdata.beat / pwindow.ydiv;
					i = -1;
					while (t < selection.getLast().time) {
						if (t >= selection.get(i + 1).time) {
							i++;
							if (timing.equals(t, selection.get(i).time)) {
								link.add(selection.get(i));
								t += pdata.beat / pwindow.ydiv;
								continue;
							}
						}
						double stime = selection.get(i).time, etime = selection
								.get(i + 1).time;
						double sx = selection.get(i).x, ex = selection
								.get(i + 1).x;
						double pos = (t - stime) / (etime - stime);
						double x = (ex - sx) * pos + sx;
						Pattern.Note node = pdata.new Note(0, t, x, 0);
						pdata.notes.add(node);
						link.add(node);
						t += pdata.beat / pwindow.ydiv;
					}
					link.add(selection.getLast());
				}
				pdata.links.add(link);
				modified = true;
			}
			break;
		}
		case "��ӵ�ָ����Link": {
			boolean check = true;
			for (Pattern.Note note : selection)
				if ((note.holdtime != 0) || (note.linkref != -1)) {
					check = false;
					break;
				}
			if (check) {
				int linkid = Integer.parseInt(JOptionPane.showInputDialog(this,
						"Link ID"));
				Pattern.Link link = pdata.links.get(linkid);
				for (Pattern.Note note : selection)
					link.add(note);
				modified = true;
			}
		}
		case "��Link���Ƴ�":
			for (Pattern.Note note : selection)
				if (note.linkref != -1) {
					Pattern.Link link = pdata.links.get(note.linkref);
					link.remove(note);
				}
			modified = true;
			break;
		case "�⿪Link":
			for (Pattern.Note note : selection)
				if (note.linkref != -1) {
					Pattern.Link link = pdata.links.get(note.linkref);
					link.removeAll();
				}
			modified = true;
			break;
		case "���Link":
			if ((selection.size() == 1) && (selection.get(0).linkref != -1)) {
				Pattern.Link link = pdata.links.get(selection.get(0).linkref);
				if (link.n >= 4) {
					String param[] = JOptionPane.showInputDialog(this,
							"��Link��node����(�ÿո����)").split(" ");
					int num[] = new int[param.length];
					int sum = 0;
					boolean check = true;
					for (int i = 0; i < param.length; i++) {
						num[i] = Integer.parseInt(param[i]);
						if (num[i] < 1) {
							check = false;
							break;
						}
						sum += num[i];
					}
					if (check && (sum == link.n)) {
						int p = 0;
						for (int i = 0; i < param.length; i++) {
							Pattern.Link nlink = pdata.new Link(
									pdata.links.size());
							for (int j = 0; j < num[i]; j++) {
								Pattern.Note node = link.nodes.get(p);
								nlink.add(node);
								p++;
							}
							pdata.links.add(nlink);
						}
						link.nodes.clear();
						link.n = 0;
						modified = true;
					}
				}
			}
			break;
		case "�·���":
			String name = JOptionPane.showInputDialog(this, "�·�������", "���� "
					+ groups.size());
			NoteGroup g = new NoteGroup(name);
			g.notes.addAll(selection);
			groups.add(g);
			gnames.put(name, g);
			JMenuItem nmenu1 = new JMenuItem(name);
			p_addtogroup.add(nmenu1);
			nmenu1.addActionListener(this);
			// JMenuItem nmenu2=new JMenuItem(name);
			m_dgroups.add(nmenu1);
			// nmenu2.addActionListener(this);
			break;
		case "ˮƽ������":
			double axis = Double.parseDouble(JOptionPane.showInputDialog(this,
					"x�Գ���", "0.5"));
			for (Pattern.Note note : selection)
				pdata.notes.add(pdata.new Note(0, note.time, 2 * axis - note.x,
						note.holdtime));
			modified = true;
			break;
		case "ɾ��":
			pdata.notes.removeAll(selection);
			modified = true;
			break;
		case "���д�����":
			pdata.errorCheck();
			break;
		}
		if (gnames.containsKey(e.getActionCommand())) {
			NoteGroup g = gnames.get(e.getActionCommand());
			filter.mode = 4;
			filter.gid = g;
			sorter.sort();
		}
		if (modified) {
			updateData();
			edit.end();
			manager.addEdit(edit);
		}
	}

	public void updateData() {
		pdata.modified();
		model.fireTableDataChanged();
	}

	public void undo() {
		manager.undo();
		spp.pdata = pdata;
		model.fireTableDataChanged();
	}

	public void valueChanged(ListSelectionEvent e) {
		selection.clear();
		int rows[] = table.getSelectedRows();
		for (int i = 0; i < rows.length; i++) {
			int num = sorter.convertRowIndexToModel(rows[i]);
			Pattern.Note note = pdata.notes.get(num);
			if (!selection.contains(note))
				selection.add(note);
		}
	}

	public void storeState(Hashtable<Object, Object> state) {
		state.put("pdata", pdata.clone());
	}

	public void restoreState(Hashtable<?, ?> state) {
		System.out.println("restoreState");
		this.pdata = (Pattern) state.get("pdata");
		model.fireTableDataChanged();
	}

	public void mousePressed(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON3)
			m_edit.getPopupMenu().show(this, e.getX(), e.getY());
	}

	public void popupMenuCanceled(PopupMenuEvent e) {
		m_edit.getPopupMenu().setInvoker(m_edit);
	}

	public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
	}

	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
	}

	public void mouseClicked(MouseEvent e) {
	}

	public void mouseReleased(MouseEvent e) {
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public class PreviewWindow extends JFrame implements MouseInputListener,
			KeyListener {
		Graphics2D g = null;
		Color linec[] = new Color[] { Color.BLACK, Color.RED, Color.BLUE,
				Color.ORANGE, Color.GRAY };
		int x = 0, y = 0;
		int xdiv = 10, ydiv = 8; // Grid
		double time = 0, page = 0;
		boolean ctrl = false;
		boolean livemapping = false;
		boolean showgrid = true;
		boolean playing = false;

		public PreviewWindow() {
			super("Preview Window");
			setSize(726, 488);
			setResizable(false);
			setVisible(true);
			g = (Graphics2D) spp.buf.getGraphics();
			new Thread(new Runnable() {
				public void run() {
					Graphics gg = getContentPane().getGraphics();
					DecimalFormat f1 = new DecimalFormat("0.00");
					DecimalFormat f2 = new DecimalFormat("0.000000");
					while (true) {
						if (!playing) {
							try {
								Thread.sleep(100);
							} catch (Exception e) {
							}
						}
						spp.paint();
						time = spp.player.getMediaTime().getSeconds();
						page = spp.calcPage(time);

						if (showgrid)
							drawGrid();
						double rx = (x - spp.XOFF) / (double) spp.WIDTH;
						g.setColor(Color.BLACK);
						g.drawString(
								"x:" + f1.format(rx) + " Time:"
										+ f2.format(getTime(y)), 10, 450);

						gg.drawImage(spp.buf, 0, 0, null);
					}
				}
			}).start();
			setFocusable(true);
			addKeyListener(this);
			addMouseListener(this);
			addMouseMotionListener(this);
		}

		public double getTime(int ypos) {
			double y = (ypos - spp.YOFF) / (double) spp.HEIGHT;
			if (page % 2 == 0)
				y = 1 - y;
			return (page + y) * pdata.beat - pdata.pshift;
		}

		public int fixX(int xpos) {
			if (showgrid) {
				double x = (xpos - spp.XOFF) / (double) spp.WIDTH;
				x = Math.round(x * pwindow.xdiv) * (1.0 / pwindow.xdiv);
				return spp.calcX(x);
			} else
				return xpos;
		}

		public int fixY(int ypos) {
			if (showgrid) {
				double ytime = timing.getNearestBeat(getTime(ypos),
						pwindow.ydiv);
				return spp.calcY(ytime);
			} else
				return ypos;
		}

		public void drawGrid() {
			// x
			g.setColor(Color.BLACK);
			for (int i = 0; i <= xdiv; i++) {
				int x = spp.calcX(i * 1.0 / xdiv);
				g.drawLine(x, spp.YOFF, x, spp.HEIGHT + spp.YOFF);
			}
			// y
			double t = timing.getBeat(page * pdata.beat - pdata.pshift,
					pwindow.ydiv);
			while (spp.calcPage(t) < page)
				t += pdata.beat / pwindow.ydiv;
			while (spp.calcPage(t) == page) {
				int liney = spp.calcY(t);
				int type = timing.getBeatType(t, pwindow.ydiv);
				g.setColor(linec[type]);
				g.drawLine(spp.XOFF, liney, spp.WIDTH + spp.XOFF, liney);
				t += pdata.beat / pwindow.ydiv;
			}
			g.setColor(Color.RED);
			g.drawOval(x - 20, y - 20, 40, 40);
		}

		public Pattern.Note selectObject(int x, int y) {
			double min = 40;
			Pattern.Note result = null;
			for (Pattern.Note n : pdata.notes)
				if ((time + pdata.beat > n.time) && (n.time >= time)) {
					int nx = spp.calcX(n.x), ny = spp.calcY(n.time);
					double dist = Math.hypot(nx - x, ny - y);
					if (dist < min) {
						min = dist;
						result = n;
					}
				}
			return result;
		}

		public void keyPressed(KeyEvent e) {
			System.out.println(e.getKeyCode());
			switch (e.getKeyCode()) {
			case 17: // Ctrl
				ctrl = true;
				break;
			case 32: // Space
				if (playing) {
					spp.player.stop();
					playing = false;
				} else {
					spp.player.start();
					playing = true;
				}
				break;
			case 37: // Arrow Left
				if (!playing)
					spp.player.setMediaTime(new Time(timing.getNearestBeat(
							time, ydiv) - pdata.beat / ydiv));
				break;
			case 38: // Arrow Up
				if (!playing)
					spp.player.setMediaTime(new Time(timing.getNearestBeat(
							time, ydiv) - pdata.beat * 2 / ydiv));
				break;
			case 39: // Arrow Right
				if (!playing)
					spp.player.setMediaTime(new Time(timing.getNearestBeat(
							time, ydiv) + pdata.beat / ydiv));
				break;
			case 40: // Arrow Down
				if (!playing)
					spp.player.setMediaTime(new Time(timing.getNearestBeat(
							time, ydiv) + pdata.beat * 2 / ydiv));
				break;
			case 88: { // x
				double rx = (x - spp.XOFF) / (double) spp.WIDTH;
				double time = getTime(y);
				StateEdit edit = new StateEdit(Editor.this, "�½�Note");
				pdata.notes
						.add(pdata.new Note(pdata.notes.size(), time, rx, 0));
				updateData();
				edit.end();
				manager.addEdit(edit);
				break;
			}
			case 90: { // z
				if (ctrl)
					undo();
				else {
					double rx = (x - spp.XOFF) / (double) spp.WIDTH;
					double time = getTime(y);
					StateEdit edit = new StateEdit(Editor.this, "�½�Note");
					pdata.notes.add(pdata.new Note(pdata.notes.size(), time,
							rx, 0));
					updateData();
					edit.end();
					manager.addEdit(edit);
				}
				break;
			}
			case 127: { // Del
				StateEdit edit = new StateEdit(Editor.this, "ɾ��");
				pdata.notes.removeAll(selection);
				selection.clear();
				updateData();
				edit.end();
				manager.addEdit(edit);
				break;
			}
			}
		}

		public void keyReleased(KeyEvent e) {
			switch (e.getKeyCode()) {
			case 17: // Ctrl
				ctrl = false;
				break;
			}
		}

		public void keyTyped(KeyEvent e) {
		}

		public void mouseClicked(MouseEvent e) {
		}

		public void mousePressed(MouseEvent e) {
			if (e.getButton() == MouseEvent.BUTTON1) {
				if (!ctrl)
					selection.clear();
				Pattern.Note note = selectObject(e.getX(), e.getY());
				if (note != null) {
					if (!selection.contains(note))
						selection.add(note);
					else
						selection.remove(note);
				}
			} else
				m_edit.getPopupMenu().show(this, e.getX(), e.getY());
			x = fixX(e.getX());
			y = fixY(e.getY());
		}

		public void mouseReleased(MouseEvent e) {
		}

		public void mouseEntered(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
		}

		public void mouseMoved(MouseEvent e) {
			x = fixX(e.getX());
			y = fixY(e.getY());
		}

		public void mouseDragged(MouseEvent e) {
			x = e.getX();
			y = e.getY();
			Pattern.Note note = selectObject(x, y);
			if ((note == null) || (!selection.contains(note)))
				return;

			StateEdit edit = new StateEdit(Editor.this);
			int nx = spp.calcX(note.x), ny = spp.calcY(note.time);
			for (Pattern.Note other : selection)
				if (other != note) {
					int ox = spp.calcX(other.x) + x - nx, oy = spp
							.calcY(other.time) + y - ny;
					other.x = (ox - spp.XOFF) / (double) spp.WIDTH;
					other.time = getTime(oy);
				}
			note.x = (x - spp.XOFF) / (double) spp.WIDTH;
			note.time = getTime(y);
			updateData();
			edit.end();
			manager.addEdit(edit);
			x = fixX(x);
			y = fixY(y);

		}
	}

	public class PatternDataTable extends AbstractTableModel {
		String colnames[] = new String[] { "NOTE", "time", "x", "holdtime",
				"linkref" };

		public int getRowCount() {
			return pdata.notes.size();
		}

		public int getColumnCount() {
			return 5;
		}

		public Object getValueAt(int row, int col) {
			Pattern.Note n = pdata.notes.get(row);
			DecimalFormat f = new DecimalFormat("0.000000");
			switch (col) {
			case 0:
				return n.id;
			case 1:
				return f.format(n.time);
			case 2:
				return f.format(n.x);
			case 3:
				return f.format(n.holdtime);
			case 4:
				return n.linkref;
			default:
				return 0;
			}
		}

		public String getColumnName(int col) {
			return colnames[col];
		}

		public boolean isCellEditable(int row, int col) {
			return (col != 0) && (col != 4); // id, linkref
		}

		public void setValueAt(Object value, int row, int col) {
			Pattern.Note n = pdata.notes.get(row);
			String val = (String) value;
			StateEdit edit = new StateEdit(Editor.this);
			switch (col) {
			case 1:
				double time = Double.parseDouble(val);
				n.time = time;
				break;
			case 2:
				double x = Double.parseDouble(val);
				n.x = x;
				break;
			case 3:
				double holdtime = Double.parseDouble(val);
				n.holdtime = holdtime;
				break;
			default:
				return;
			}
			updateData();
			edit.end();
			manager.addEdit(edit);
		}

	}

	public class DataFilter extends RowFilter<Object, Object> {
		int mode = 0;
		NoteGroup gid = null;

		public boolean include(Entry<? extends Object, ? extends Object> entry) {
			int id = Integer.parseInt(entry.getStringValue(0));
			double holdtime = Double.parseDouble(entry.getStringValue(3));
			int linkref = Integer.parseInt(entry.getStringValue(4));
			switch (mode) {
			case 0:
				return true;
			case 1:// Circle
				return (holdtime == 0) && (linkref == -1);
			case 2:// Hold
				return holdtime > 0;
			case 3:// Link
				return linkref != -1;
			case 4:// Group
				if (gid == null)
					return false;
				else
					return gid.notes.contains(pdata.notes.get(id));
			default:
				return true;
			}
		}
	}

	public static void main(String args[]) throws Exception {
		new Editor();
	}
}