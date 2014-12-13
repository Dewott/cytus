package cytus;

import cytus.animation.*;
import java.util.*;
import java.io.*;
import java.text.*;
import java.awt.*;
import java.awt.image.*;
import javax.imageio.*;
import javax.media.*;
import javazoom.jl.converter.*;

public class NoteChartPlayer {
	final static String path = "assets/songs/";
	public static LinkedHashMap<String, Integer> prefs = new LinkedHashMap<String, Integer>();
	public static int WIDTH = 960, HEIGHT = 640;
	public static double SIZE_FIX = HEIGHT / 640.0;
	static {
		File f = new File("preferences.txt");
		if (!f.exists()) {
			prefs.put("clickfx", 1);
			prefs.put("quality", 1);
			prefs.put("bg", 1);
			prefs.put("popupmode", 2);
			prefs.put("convertmp3", 1);
			prefs.put("nospecialpaint", 0);
			prefs.put("width", 720);
			prefs.put("height", 480);
			prefs.put("showid", 0);
			prefs.put("inputmode", 0);
			try {
				PrintWriter p = new PrintWriter(new FileWriter(f));
				for (Map.Entry<String, Integer> entry : prefs.entrySet())
					p.println(entry.getKey() + "=" + entry.getValue());
				p.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			try {
				BufferedReader in = new BufferedReader(new FileReader(f));
				String str = in.readLine();
				while (str != null) {
					String pair[] = str.split("=");
					prefs.put(pair[0], Integer.parseInt(pair[1]));
					str = in.readLine();
				}
				in.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (!prefs.containsKey("clickfx"))
				prefs.put("clickfx", 1);
			if (!prefs.containsKey("quality"))
				prefs.put("quality", 1);
			if (!prefs.containsKey("bg"))
				prefs.put("bg", 1);
			if (!prefs.containsKey("popupmode"))
				prefs.put("popupmode", 2);
			if (!prefs.containsKey("convertmp3"))
				prefs.put("convertmp3", 1);
			if (!prefs.containsKey("nospecialpaint"))
				prefs.put("nospecialpaint", 0);
			if (!prefs.containsKey("width"))
				prefs.put("width", 720);
			if (!prefs.containsKey("height"))
				prefs.put("height", 480);
			if (!prefs.containsKey("showid"))
				prefs.put("showid", 0);
			if (!prefs.containsKey("inputmode"))
				prefs.put("inputmode", 0);
		}
		WIDTH = prefs.get("width");
		HEIGHT = prefs.get("height");
		SIZE_FIX = HEIGHT / 640.0;
		try {
			cytus.animation.SpriteLibrary.load();
			cytus.animation.AnimationPreset.load();
			cytus.animation.FontLibrary.load();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static int PWIDTH = HEIGHT * 45 / 32, PHEIGHT = HEIGHT * 25 / 32;
	public static int PXOFF = (WIDTH - PWIDTH) / 2,
			PYOFF = (HEIGHT - PHEIGHT) / 2;

	public double pshift = 0, beat = 0, time = 0;
	public int page = 0;
	int liney = 0, combo = 0, maxcombo = 0, ncount = 0;
	double score = 0, tp = 0;

	public NoteChart pdata = null;
	public LinkedList<Note> notes = new LinkedList<Note>();
	public LinkedList<Note> copy = new LinkedList<Note>();
	LinkedList<Animation> animqueue = new LinkedList<Animation>();

	public BufferedImage buf = new BufferedImage(WIDTH, HEIGHT,
			BufferedImage.TYPE_INT_RGB);
	Graphics2D gg = null;
	Image bg = null, line = null;
	ComboSmallPop pop = null;
	MaskBeat mask = null;
	ComboAnimation comboanim = null;
	UserInput input = null;
	int result[] = new int[4];

	public Player player = null; // Media Player
	JSPlayer sound = null;

	public NoteChartPlayer(String name, String diff) throws Exception {
		BufferedReader in = new BufferedReader(new FileReader(path + name + "/"
				+ name + "." + diff + ".txt"));

		if (in.readLine().equals("VERSION 2"))
			pdata = NoteChartReader2.read(in);
		else
			pdata = NoteChartReader1.read(in);

		in.close();

		loadNoteChart(pdata);

		if (prefs.get("convertmp3") == 1) {
			new Converter().convert(path + name + "/" + name + ".mp3",
					"temp.wav");
			player = Manager.createRealizedPlayer(new MediaLocator(new File(
					"temp.wav").toURI().toURL()));
		} else
			player = Manager.createRealizedPlayer(new MediaLocator(new File(
					path + name + "/" + name + ".mp3").toURI().toURL()));

		bg = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
		Graphics g = bg.getGraphics();
		if (prefs.get("bg") == 1) {
			BufferedImage bgfile = null;
			/*
			 * try { if (new File(path + name + "/" + name +
			 * "_bg.png").exists()) bgfile = ImageIO.read(new File(path + name +
			 * "/" + name + "_bg.png")); else if (new File(path + name +
			 * "/bg.png").exists()) bgfile = ImageIO.read(new File(path + name +
			 * "/bg.png")); else bgfile = ImageIO.read(new File(path + name +
			 * "/" + name + ".png"));
			 * 
			 * bgfile = ImageUtil.scale(bgfile, -1, HEIGHT); g.drawImage(bgfile,
			 * (WIDTH - bgfile.getWidth()) / 2, 0, null); } catch (Exception e)
			 * { e.printStackTrace(); }
			 */
			try {
				Sprite cover = new cytus.cover.SelectCover(name);
				BufferedImage buf = new BufferedImage(1024, 683,
						BufferedImage.TYPE_INT_ARGB);
				cover.paint((Graphics2D) buf.getGraphics(), 10); // time=10s
				bgfile = ImageUtil.scale(buf, WIDTH, HEIGHT);
				g.drawImage(bgfile, 0, 0, null);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, WIDTH, HEIGHT);
		}
		g.dispose();

		line = SpriteLibrary.get("scanline");
		try {
			sound = new JSPlayer("assets/sounds/beat1.wav");
		} catch (Exception e) {
			e.printStackTrace();
			sound.mute();
		}
		if (prefs.get("clickfx") == 0)
			sound.mute();

		mask = new MaskBeat(pshift, beat / 2);

		gg = (Graphics2D) buf.getGraphics();
		gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		if (prefs.get("quality") > 1)
			gg.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
					RenderingHints.VALUE_INTERPOLATION_BICUBIC);

		if (prefs.get("quality") > 2)
			gg.setRenderingHint(RenderingHints.KEY_RENDERING,
					RenderingHints.VALUE_RENDER_QUALITY);

		gg.setStroke(new BasicStroke((int) (7 * SIZE_FIX)));
		gg.setColor(Color.BLACK);
	}

	public void loadNoteChart(NoteChart pdata) {
		this.pdata = pdata;

		beat = pdata.beat;
		pshift = pdata.pshift;
		ncount = pdata.notes.size();
		notes.clear();
		for (NoteChart.Note pnote : pdata.notes)
			if (pnote.linkref == -1) {
				if (pnote.holdtime == 0)
					this.notes.add(new Circle(this, pnote.id, calcX(pnote.x),
							calcY(pnote.time), pnote.time));
				else
					this.notes.add(new Hold(this, pnote.id, calcX(pnote.x),
							calcY(pnote.time), pnote.time, pnote.holdtime));
			}
		for (NoteChart.Link plink : pdata.links) {
			Link link = new Link(this);
			for (int i = 0; i < plink.n; i++) {
				NoteChart.Note node = plink.nodes.get(i);
				link.nodes.add(link.new Node(node.id, calcX(node.x),
						calcY(node.time), node.time));
			}
			link.recalc();
			this.notes.add(link);
		}

		Note t[] = new Note[notes.size()];
		notes.toArray(t);
		Arrays.sort(t, new Comparator<Note>() {
			public int compare(Note n1, Note n2) {
				return Double.compare(n1.stime, n2.stime);
			}
		});
		notes.clear();
		Collections.addAll(notes, t);
		copy.clear();
		copy.addAll(notes);
	}

	public void addAnimation(Animation a) {
		animqueue.add(a);
	}

	public void removeAnimation(Animation a) {
		animqueue.remove(a);
	}

	public void addCombo(int judgement) {
		if (judgement == -1) { // Miss
			combo = 0;
			result[3]++;
			return;
		}
		combo++;
		double sratio = 0, tpratio = 0;
		switch (judgement) {
		case 0: // Perfect TP100
			sratio = 1;
			tpratio = 1;
			result[0]++;
			break;
		case 1: // Perfect TP70
			sratio = 1;
			tpratio = 0.7;
			result[0]++;
			break;
		case 2: // Good
			sratio = 0.7;
			tpratio = 0.3;
			result[1]++;
			break;
		case 3: // Bad
			sratio = 0.3;
			tpratio = 0;
			combo = 0;
			result[2]++;
			break;
		}
		if (combo > maxcombo)
			maxcombo = combo;
		score += 900000.0 / ncount * sratio + combo * 200000.0 / ncount
				/ (ncount + 1);
		tp += 100.0 / ncount * tpratio;
		pop = new ComboSmallPop(time);

		try {
			sound.start();
		} catch (Exception e) {
		}

		if ((combo > 0) && (combo % 25 == 0))
			comboanim = new ComboAnimation(combo, time);
	}

	public int calcPage(double time) {
		return (int) ((time + pshift) / beat);
	}

	public int calcX(double x) {
		return (int) (x * PWIDTH + PXOFF);
	}

	public int calcY(double time) {
		int cpage = calcPage(time);
		double y = (time + pshift) / beat - cpage;
		if (cpage % 2 == 0)
			y = 1 - y;
		return (int) (y * PHEIGHT + PYOFF);
	}

	public void start(Graphics2D g) throws Exception {
		player.start();
		while (true) {
			time = player.getMediaTime().getSeconds();
			int drawtime = paint();
			if (prefs.get("inputmode") == 1)
				input.doEvent();
			g.drawImage(buf, 0, 0, null);
		}
	}

	public void restart() {
		notes.clear();
		animqueue.clear();
		Collections.sort(notes);
		ncount = 0;
		for (Note n : notes) {
			if (n instanceof Link) {
				Link l = (Link) n;
				l.recalc();
				ncount += l.n;
			} else
				ncount++;

			n.judgement = -1;
			n.judgetime = -1;
			if (n instanceof Hold) {
				Hold h = (Hold) n;
				h.judgetime1 = -1;
				h.judgetime2 = -1;
			}
		}
		result[0] = 0;
		result[1] = 0;
		result[2] = 0;
		result[3] = 0;
		combo = 0;
		maxcombo = 0;
		score = 0;
		tp = 0;
	}

	public int paint() {
		long mtime1 = System.nanoTime();
		gg.drawImage(bg, 0, 0, null);
		gg.setComposite(AlphaComposite.SrcOver.derive(0.75f));
		gg.setColor(Color.WHITE);
		gg.fillRect(0, 0, WIDTH, HEIGHT);
		gg.setColor(Color.BLACK);
		gg.setComposite(AlphaComposite.SrcOver);

		BufferedImage title = SpriteLibrary.get("gameplay_title");
		BufferedImage mask1 = SpriteLibrary.get("gameplay_title_mask");
		BufferedImage mask2 = SpriteLibrary.get("gameplay_bg_mask_2");
		BufferedImage white = SpriteLibrary.get("gameplay_bg_mask");

		gg.drawImage(white, 0, 0, WIDTH, HEIGHT / 10, null);
		gg.drawImage(mask2, 0, 0, WIDTH, HEIGHT / 10, null);
		gg.drawImage(mask1, 0, 0, null);
		gg.drawImage(mask1, WIDTH, 0, WIDTH - mask1.getWidth(),
				mask1.getHeight(), 0, 0, mask1.getWidth(), mask1.getHeight(),
				null);
		gg.drawImage(title, WIDTH / 2 - title.getWidth() / 2, 0, null);

		if (combo > 1) {
			BufferedImage text = SpriteLibrary.get("combo_small_text");
			BufferedImage bg = SpriteLibrary.get("combo_small_bg");
			double scale = pop != null ? pop.scale(time) : 1;
			int w1 = (int) (250 * SIZE_FIX * scale);
			gg.drawImage(bg, WIDTH / 2 - w1 / 2, (int) (44 * SIZE_FIX), w1,
					(int) (28 * SIZE_FIX), null);
			gg.drawImage(text, (int) (WIDTH / 2 - 110 * SIZE_FIX),
					(int) (44 * SIZE_FIX), (int) (92 * SIZE_FIX),
					(int) (27 * SIZE_FIX), null);
			BufferedImage cp = FontLibrary.getComboSmall(combo);
			int w2 = (int) (cp.getWidth() * scale), h2 = (int) (cp.getHeight() * scale);
			gg.drawImage(cp, (int) (WIDTH / 2 + 58 * SIZE_FIX) - w2 / 2,
					(int) (58 * SIZE_FIX) - h2 / 2, w2, h2, null);
		}

		if (mask != null)
			mask.paint(gg, time);

		page = calcPage(time);
		liney = calcY(time);

		// inputmode=1 : Throws java.util.ConcurrentModificationException
		LinkedList<Animation> del = new LinkedList<Animation>();
		for (int i = 0; i < animqueue.size(); i++) {
			Animation anim = animqueue.get(i);
			if (anim.getEndTime() < time)
				del.add(anim);
			else if (anim.getStartTime() <= time)
				anim.paint(gg, time);
		}
		animqueue.removeAll(del);

		if (comboanim != null)
			comboanim.paint(gg, time);

		if (notes.size() > 0) {
			int end = -1;
			int i = 0;
			for (i = 0; i < notes.size(); i++) {
				Note n = notes.get(i);
				if (n.stime > time + beat)
					break;
				end = i;
			}
			for (i = end; i >= 0; i--) {
				Note n = notes.get(i);
				n.paint(gg);
			}
		}

		gg.drawImage(line, 0, liney - (int) (24 * SIZE_FIX), null);

		BufferedImage sp = FontLibrary.getScore(score);
		gg.drawImage(sp, WIDTH - sp.getWidth(), HEIGHT / 50, null);

		long mtime2 = System.nanoTime();
		double fps = 1e9 / (mtime2 - mtime1);
		String info = "Time:"
				+ new java.text.DecimalFormat("0.000").format(time);
		info += " TP:" + new java.text.DecimalFormat("00.00").format(tp);
		info += " fps:" + (int) fps;

		gg.drawString(info, 10, 20);
		return (int) (mtime2 - mtime1);
	}
}
