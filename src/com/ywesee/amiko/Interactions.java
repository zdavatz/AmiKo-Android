package com.ywesee.amiko;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.TreeMap;

import com.ywesee.amiko.de.R;

import android.content.Context;
import android.util.Log;

public class Interactions {
	private static final String TAG = "Interactions"; // Tag for LogCat window	
	private Map<String, Medication> m_med_basket = new TreeMap<String, Medication>();
	private Map<String, String> m_interactions_map = null;
	private List<String> m_section_titles_list = null;
	private String m_interactions_html_str = null;
	private String m_css_interactions_str = null;
	private String m_js_deleterow_str = null;
	
	public Interactions(Context context) {
		// Load drug interaction map
		String interactions_file =  context.getApplicationInfo().dataDir + "/databases/" 
				+ Constants.appInteractionsFile(); 
		m_interactions_map = readFromCsvToMap(interactions_file);
		// Load interactions css style sheet
		m_css_interactions_str = "<style>" 
				+ Utilities.loadFromAssetsFolder(context, "interactions_css.css", "UTF-8") + "</style>";
		// Load delete row javascript
		m_js_deleterow_str = Utilities.loadFromAssetsFolder(context, "deleterow.js", "UTF-8");
	}
	
	public void addToBasket(String title, Medication med) {
		if (m_med_basket!=null)
			m_med_basket.put(title, med);
	}
	
	public void deleteFromBasket(String row_key) {
		if (m_med_basket!=null && m_med_basket.containsKey(row_key))
			m_med_basket.remove(row_key);
	}
	
	public void clearBasket() {
		if (m_med_basket!=null)
			m_med_basket.clear();
	}
	
	public String getInteractionsHtml() {
		return m_interactions_html_str;
	}
	
	public List<String> getInteractionsTitles() {
		return m_section_titles_list;
	}
	
	public void updateInteractionsHtml() {
		String basket_html_str = medBasketHtml();
		String interactions_html_str = interactionsHtml();
		String foot_note_html_str = footNoteHtml();

		// Update main interactions html string
		m_interactions_html_str = "<html><head><meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />"
				+ "<script type=\"text/javascript\">" + m_js_deleterow_str + "</script>" 
				+ m_css_interactions_str + "</head><body>" 
				+ basket_html_str + "<br>" + interactions_html_str + "<br>" + foot_note_html_str 
				+ "</body></div></html>";
	}
	
	private String medBasketHtml() {
		String basket_html_str = "";
		String atc_code1 = "";
		String name1 = "";
		String[] m_code1 = null;
		int med_counter = 1;
		
		// Build interaction basket table
		if (m_med_basket!=null && m_med_basket.size()>0) {			
			if (Constants.appLanguage().equals("de")) 
				basket_html_str = "<div id=\"Medikamentenkorb\"><fieldset><legend>Medikamentenkorb</legend></fieldset></div><table id=\"InterTable\" width=\"100%25\">";
			else if (Constants.appLanguage().equals("fr"))
				basket_html_str = "<div id=\"Medikamentenkorb\"><fieldset><legend>Panier des Médicaments</legend></fieldset></div><table id=\"InterTable\" width=\"100%25\">";			
			for (Map.Entry<String, Medication> entry1 : m_med_basket.entrySet()) {
				m_code1 = entry1.getValue().getAtcCode().split(";");
				atc_code1 = "k.A.";
				name1 = "k.A.";
				if (m_code1.length>1) {
					atc_code1 = m_code1[0];
					name1 = m_code1[1];
				}
				basket_html_str += "<tr>";
				// Source folder for the images is /res/drawable
				String trash_icon = "<input type=\"image\" src=\"trash_icon.png\" onclick=\"deleterow('Interaktionen',this)\" />";
				basket_html_str += "<td>" + med_counter + "</td>" 
						+ "<td>" + entry1.getKey() + " </td> " 
						+ "<td>" + atc_code1 + "</td>" 
						+ "<td>" + name1 + "</td>"
						+ "<td align=\"right\">" + trash_icon + "</td>";
				basket_html_str += "</tr>";
				med_counter++;
			}
			if (Constants.appLanguage().equals("de"))
				basket_html_str += "</table><div id=\"Delete_all\"><input type=\"button\" value=\"Korb leeren\" onclick=\"deleterow('Delete_all',this)\" /></div>";
			else if (Constants.appLanguage().equals("fr"))
				basket_html_str += "</table><div id=\"Delete_all\"><input type=\"button\" value=\"Tout supprimer\" onclick=\"deleterow('Delete_all',this)\" /></div>";				
		} else {
			// Medikamentenkorb ist leer
			if (Constants.appLanguage().equals("de"))
				basket_html_str = "<div>Ihr Medikamentenkorb ist leer.<br><br></div>";
			else if (Constants.appLanguage().equals("fr"))
				basket_html_str = "<div>Votre panier de médicaments est vide.<br><br></div>";
		}
		
		return basket_html_str;
	}
		
	private String interactionsHtml() {
		String interactions_html_str = "";
		String atc_code1 = "";
		String atc_code2 = "";
		String[] m_code1 = null;
		String[] m_code2 = null;
	
		
		// Build list of interactions
		m_section_titles_list = new ArrayList<String>();
		// Add table to section titles
		if (Constants.appLanguage().equals("de"))
			m_section_titles_list.add("Interaktionen");
		else if (Constants.appLanguage().equals("fr"))
			m_section_titles_list.add("Interactions");
		
		if (m_med_basket.size()>1) {
			if (Constants.appLanguage().equals("de"))
				interactions_html_str = "<fieldset><legend>Bekannte Interaktionen</legend></fieldset>";
			else if (Constants.appLanguage().equals("fr"))
				interactions_html_str = "<fieldset><legend>Interactions Connues</legend></fieldset>";				
			for (Map.Entry<String, Medication> entry1 : m_med_basket.entrySet()) {
				m_code1 = entry1.getValue().getAtcCode().split(";");
				if (m_code1.length>1) {
					// Get ATC code of first drug, make sure to get the first in
					// the list (the second one is not used)
					atc_code1 = m_code1[0].split(",")[0];
					for (Map.Entry<String, Medication> entry2 : m_med_basket.entrySet()) {
						m_code2 = entry2.getValue().getAtcCode().split(";");
						if (m_code2.length > 1) {
							// Get ATC code of second drug
							atc_code2 = m_code2[0];
							if (atc_code1 != null && atc_code2 != null && !atc_code1.equals(atc_code2)) {
								// Get html interaction content from drug
								// interactions map
								String inter = m_interactions_map.get(atc_code1	+ "-" + atc_code2);
								if (inter != null) {
									inter = inter.replaceAll(atc_code1,	entry1.getKey());
									inter = inter.replaceAll(atc_code2,	entry2.getKey());
									interactions_html_str += (inter + "");
									// Add title to section title list
									if (!inter.isEmpty())
										m_section_titles_list.add("<html>" + entry1.getKey() + " &rarr; "
												+ entry2.getKey() + "</html>");
								}
							}
						}
					}
				}
			}
			// Add note to indicate that there are no interactions
			if (m_section_titles_list.size()<2) {
				if (Constants.appLanguage().equals("de")) {
					interactions_html_str = "<p class=\"paragraph0\">Zur Zeit sind keine Interaktionen zwischen diesen Medikamenten bekannt.</p>" +
							"<div id=\"Delete_all\"><input type=\"button\" value=\"Interaktion melden\" onclick=\"deleteRow('Notify_interaction',this)\" /></div><br>";
				} else if (Constants.appLanguage().equals("fr")) {
					interactions_html_str = "<p class=\"paragraph0\">Jusqu’ici il n’y pas d’interaction connue entre les médicaments.</p>" +
							"<div id=\"Delete_all\"><input type=\"button\" value=\"Signaler une interaction\" onclick=\"deleteRow('Notify_interaction',this)\" /></div><br>";
				} 
			} else {
				interactions_html_str = "<br>";
			}
		}
		return interactions_html_str;
	}
	
	private String footNoteHtml() {
	    /*
	     Risikoklassen
	     -------------
		     A: Keine Massnahmen notwendig (grün)
		     B: Vorsichtsmassnahmen empfohlen (gelb)
		     C: Regelmässige Überwachung (orange)
		     D: Kombination vermeiden (pinky)
		     X: Kontraindiziert (hellrot)
		     0: Keine Angaben (grau)
	    */

		String legend = "";
		if (Constants.appLanguage().equals("de")) {
			legend = "<fieldset><legend>Fussnoten</legend></fieldset>" +
					"<p class=\"footnote\">1. Farblegende: </p>" +
					"<table id=\"Farblegende\" style=\"background-color:#ffffff;\" cellpadding=\"3px\" width=\"100%25\">" +
					"<tr bgcolor=\"#caff70\"><td align=\"center\">A</td><td>Keine Massnahmen notwendig</td></tr>" +
					"<tr bgcolor=\"#ffec8b\"><td align=\"center\">B</td><td>Vorsichtsmassnahmen empfohlen</td></tr>" +
					"<tr bgcolor=\"#ffb90f\"><td align=\"center\">C</td><td>Regelmässige Überwachung</td></tr>" +
					"<tr bgcolor=\"#ff82ab\"><td align=\"center\">D</td><td>Kombination vermeiden</td></tr>" +
					"<tr bgcolor=\"#ff6a6a\"><td align=\"center\">X</td><td>Kontraindiziert</td></tr>" +
					"</table>" + 
					"<p class=\"footnote\">2. Datenquelle: Public Domain Daten von EPha.ch.</p>" +
					"<p class=\"footnote\">3. Unterstützt durch: IBSA Institut Biochimique SA.</p>";
		} else if (Constants.appLanguage().equals("fr")) {
			legend = "<fieldset><legend>Notes</legend></fieldset>" +
					"<p class=\"footnote\">1. Légende des couleurs: </p>" +
					"<table id=\"Farblegende\" style=\"background-color:#ffffff;\" cellpadding=\"3px\" width=\"100%25\">" +
					"<tr bgcolor=\"#caff70\"><td align=\"center\">A</td><td>Aucune mesure nécessaire</td></tr>" +
					"<tr bgcolor=\"#ffec8b\"><td align=\"center\">B</td><td>Mesures de précaution sont recommandées</td></tr>" +
					"<tr bgcolor=\"#ffb90f\"><td align=\"center\">C</td><td>Doit être régulièrement surveillée</td></tr>" +
					"<tr bgcolor=\"#ff82ab\"><td align=\"center\">D</td><td>Eviter la combinaison</td></tr>" +
					"<tr bgcolor=\"#ff6a6a\"><td align=\"center\">X</td><td>Contre-indiquée</td></tr>" +
					"</table>" +
					"<p class=\"footnote\">2. Source des données : données du domaine publique de EPha.ch.</p>" +
					"<p class=\"footnote\">3. Soutenu par : IBSA Institut Biochimique SA.</p>";
		}
		return legend;		
	}
		
	private Map<String,String> readFromCsvToMap(String filename) 
	{
		Map<String, String> map = new TreeMap<String, String>();
		try {
			File file = new File(filename);
			if (!file.exists()) 
				return null;
			FileInputStream fis = new FileInputStream(filename);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
			String line;
			while ((line = br.readLine()) != null) {
				String token[] = line.split("\\|\\|");
				map.put(token[0] + "-" + token[1], token[2]);
			}
			br.close();
		} catch (Exception e) {
			System.err.println(">> Error in reading csv file");
		}
		
		return map;
	}	
}
