package org.thatmadhacker.svg2gcode;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Main {

	public static void main(String[] args) throws Exception {
		Scanner in = new Scanner(System.in);
		System.out.print("SVG Path: ");
		File image = new File(in.nextLine());
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document document = db.parse(image);

		NodeList nList = document.getDocumentElement().getChildNodes();

		List<String> gcode = new ArrayList<String>();

		System.out.print("Bed width: ");
		int bWidth = Integer.valueOf(in.nextLine());
		System.out.print("Bed length: ");
		int bLength = Integer.valueOf(in.nextLine());

		System.out.print("Line width: ");
		double lineWidth = Double.valueOf(in.nextLine());

		System.out.print("Start height: ");
		int sHeight = Integer.valueOf(in.nextLine());

		gcode.add("M82");
		gcode.add("G21");
		gcode.add("G90");
		gcode.add("M82");
		gcode.add("M107");
		gcode.add("G28 X0Y0");
		gcode.add("G28 Z" + sHeight);

		int svgWidth = Integer.valueOf(document.getDocumentElement().getAttribute("width"));
		int svgHeight = Integer.valueOf(document.getDocumentElement().getAttribute("height"));

		double xScale = (double) bWidth / (double) svgWidth;
		double yScale = (double) bLength / (double) svgHeight;

		for (int i = 0; i < nList.getLength(); i++) {
			Node node = nList.item(i);
			String name = node.getNodeName();
			if (node instanceof Element) {
				Element e = (Element) node;
				NamedNodeMap map = e.getAttributes();
				if (name.equals("rect")) {
					int width = Integer.MIN_VALUE;
					int height = Integer.MIN_VALUE;
					int x = Integer.MIN_VALUE;
					int y = Integer.MIN_VALUE;
					int rx = 0;
					for (int i1 = 0; i1 < map.getLength(); i1++) {
						Node n1 = map.item(i1);
						switch (n1.getNodeName()) {
						case "x":
							x = Integer.valueOf(n1.getNodeValue());
							break;
						case "y":
							y = Integer.valueOf(n1.getNodeValue());
							break;
						case "width":
							width = Integer.valueOf(n1.getNodeValue());
							break;
						case "height":
							height = Integer.valueOf(n1.getNodeValue());
							break;
						case "rx":
							rx = Integer.valueOf(n1.getNodeValue());
							break;
						}
					}
					if (x == Integer.MIN_VALUE || y == Integer.MIN_VALUE || width == Integer.MIN_VALUE
							|| height == Integer.MIN_VALUE) {
						System.err.println("Error in SVG file!");
						System.exit(1);
					}

					x = (int) (x * xScale);
					y = (int) (y * yScale);
					width = (int) (width * xScale);
					height = (int) (height * yScale);

					int adjustedWidth = width - rx;
					int adjustedHeight = height - rx;
					gcode.add("G00 X" + (x + rx) + "Y" + y);
					gcode.add("G01 X" + (x + adjustedWidth) + "Y" + y);
					gcode.add("G03 X" + (x + width) + "Y" + (y + rx) + " R" + rx);
					gcode.add("G01 X" + (x + width) + "Y" + (y + adjustedHeight));
					gcode.add("G03 X" + (x + adjustedWidth) + "Y" + (y + height) + " R" + rx);
					gcode.add("G01 X" + (x + rx) + "Y" + (y + height));
					gcode.add("G03 X" + x + "Y" + (y + adjustedHeight) + " R" + rx);
					gcode.add("G01 X" + x + "Y" + (y + rx));
					gcode.add("G03 X" + (x + rx) + "Y" + y + " R" + rx);
					boolean atLeft = true;
					for (double y1 = y; y1 < y + height; y1 += lineWidth) {
						double width2 = x + width;
						if (y1 < y + rx) {
							double width3 = rx-Math.sqrt(Math.pow(rx, 2) - Math.pow(rx-(y1-y), 2));
							double x2 = (atLeft ? x+width3 : x + width - width3);
							double x3 = (!atLeft? x + width3: x+width-width3);
							gcode.add("G00 X"+x2+"Y"+y1);
							gcode.add("G01 X"+x3);
						}else if(y1 > y + height - rx) {
							double width3 = rx-Math.sqrt(Math.pow(rx, 2) - Math.pow(rx+(y1-(y+height)), 2));
							double x2 = (atLeft ? x+width3 : x + width - width3);
							double x3 = (!atLeft? x + width3: x+width-width3);
							gcode.add("G00 X"+x2+"Y"+y1);
							gcode.add("G01 X"+x3);
						}else {
							double x2 = (atLeft ? width2 : x);
							gcode.add("G00 Y"+y1);
							gcode.add("G01 X"+x2);
						}
						atLeft = !atLeft;
					}

				} else if (name.equals("circle")) {

				}
			}
		}
		System.out.print("Output file: ");
		File outputFile = new File(in.nextLine());
		outputFile.delete();
		outputFile.createNewFile();
		PrintWriter out = new PrintWriter(new FileWriter(outputFile));
		for (String s : gcode) {
			out.println(s);
		}
		out.close();
		in.close();
	}

}
