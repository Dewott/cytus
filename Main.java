package cytus;

import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.io.*;

public class Main extends JFrame {
	public Main(String str) throws Exception {
		super("Cytus");
		Locale.setDefault(Locale.CHINESE);
		System.setProperty("sun.java2d.opengl", "true");
		NoteChartPlayer p = new NoteChartPlayer(str, "hard");
		setSize(NoteChartPlayer.WIDTH + 6, NoteChartPlayer.HEIGHT + 8);
		setResizable(false);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((d.width - getWidth()) / 2, (d.height - getHeight()) / 2);
		setVisible(true);
		p.input = new UserInput(p, getContentPane());
		Graphics2D g = (Graphics2D) getContentPane().getGraphics();
		p.start(g);
	}

	public static void main(String args[]) {
		try {
			new Main(args[0]);
		} catch (Exception e) {
			e.printStackTrace();
			// System.exit(1);
		}
	}
}
