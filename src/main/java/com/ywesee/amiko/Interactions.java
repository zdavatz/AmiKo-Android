package com.ywesee.amiko;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Interactions {
  private static final String TAG = "Interactions"; // Tag for LogCat window
  private Context mContext = null;
  private Map<String, Medication> m_med_basket = new TreeMap<String, Medication>();
  private Map<String, String> m_interactions_map = null;
  private List<String> m_section_ids_list = null;
  private List<String> m_section_titles_list = null;
  private String m_interactions_html_str = null;
  private JSONObject m_epha_response = null;
  private String m_css_interactions_str = null;
  private String m_js_interactions_str = null;
  public Runnable htmlUpdated = null;

  public Interactions(Context context) {
    mContext = context;

    if (mContext!=null) {
      // Load interactions css style sheet
      if (Utilities.isTablet(mContext))
        m_css_interactions_str = "<style>" + Utilities.loadFromAssetsFolder(mContext, "interactions_css.css", "UTF-8") + "</style>";
      else
        m_css_interactions_str = "<style>" + Utilities.loadFromAssetsFolder(mContext, "interactions_css_phone.css", "UTF-8") + "</style>";
      // Load delete row javascript
      m_js_interactions_str = Utilities.loadFromAssetsFolder(mContext, "interactions.js", "UTF-8");
    }
  }

  public void loadCsv() {
    if (mContext!=null) {
      // Load drug interaction map
      String interactions_file =  mContext.getApplicationInfo().dataDir + "/databases/"
          + Constants.appInteractionsFile();
      m_interactions_map = readFromCsvToMap(interactions_file);
    }
  }

  public void addToBasket(String title, Medication med) {
    if (m_med_basket!=null) {
      m_med_basket.put(title, med);
    }
    callEPha();
  }

  public void deleteFromBasket(String row_key) {
    if (m_med_basket!=null && m_med_basket.containsKey(row_key)) {
      m_med_basket.remove(row_key);
    }
    callEPha();
  }

  public void clearBasket() {
    if (m_med_basket!=null) {
      m_med_basket.clear();
    }
    callEPha();
  }

  public String getInteractionsHtml() {
    return m_interactions_html_str;
  }

  public List<String> getInteractionsTitles() {
    return m_section_titles_list;
  }

  public List<String> getInteractionTitleIds() {
    return m_section_ids_list;
  }

  public void callEPha() {
    JSONArray jsonArray = new JSONArray();
    if (m_med_basket.isEmpty()) {
      m_epha_response = null;
      return;
    }
    for (Map.Entry<String, Medication> entry1 : m_med_basket.entrySet()) {
      JSONObject jsonObject = new JSONObject();
      try {
        jsonObject.put("type", "drug");
        String[] packages = entry1.getValue().getPackages().split("\\|");
        String gtin = packages[9];
        jsonObject.put("gtin", gtin);
        jsonArray.put(jsonObject);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    AndroidNetworking.post("https://api.epha.health/clinic/advice/" + Constants.appLanguage() + "/")
            .addHeaders("Content-Type", "application/json")
            .addJSONArrayBody(jsonArray)
            .build()
            .getAsJSONObject(new JSONObjectRequestListener() {
              @Override
              public void onResponse(JSONObject response) {
                try {
                  int code = response.getJSONObject("meta").getInt("code");
                  if (code >= 200 && code < 300) {
                    JSONObject jsonObj = response.getJSONObject("data");
                    m_epha_response = jsonObj;
                  }
                  updateInteractionsHtml();
                } catch (JSONException e) {
                  e.printStackTrace();
                }
              }

              @Override
              public void onError(ANError anError) {
                anError.printStackTrace();
              }
            });
  }

  public void openEPha() {
    if (m_epha_response == null) return;
    try {
      String link = m_epha_response.getString("link");
      mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private String htmlForEpha(JSONObject j) throws JSONException {
    String lang = Constants.appLanguage();
    int safety = j.getInt("safety");
    int kinetic = j.getJSONObject("risk").getInt("kinetic");
    int qtc = j.getJSONObject("risk").getInt("qtc");
    int warning = j.getJSONObject("risk").getInt("warning");
    int serotonerg = j.getJSONObject("risk").getInt("serotonerg");
    int anticholinergic = j.getJSONObject("risk").getInt("anticholinergic");
    int adverse = j.getJSONObject("risk").getInt("adverse");

    String html_str = "";

    if (lang.equals("de")) {
      html_str += "Sicherheit<BR>";
      html_str += "<p class='risk-description'>Je höher die Sicherheit, desto sicherer die Kombination.</p>";
    } else {
      html_str += "Sécurité<BR>";
      html_str += "<p class='risk-description'>Plus la sécurité est élevée, plus la combinaison est sûre.</p>";
    }

    html_str += "<div class='risk'>100";
    html_str += "<div class='gradient'>" +
            "<div class='pin' style='left: " + (100-safety) + "%'>" + safety + "</div>" +
            "</div>";
    html_str += "0</div><BR><BR>";

    if (lang.equals("de")) {
      html_str += "Risikofaktoren<BR>";
      html_str += "<p class='risk-description'>Je tiefer das Risiko, desto sicherer die Kombination.</p>";
    } else {
      html_str += "Facteurs de risque<BR>";
      html_str += "<p class='risk-description'>Plus le risque est faible, plus la combinaison est sûre.</p>";
    }

    html_str += "<table class='risk-table'>";
    html_str += "<tr><td class='risk-name'>";
    html_str += lang.equals("de") ? "Pharmakokinetik" : "Pharmacocinétique";
    html_str += "</td>";
    html_str += "<td>";
    html_str += "<div class='risk'>0";
    html_str += "<div class='gradient'><div class='pin' style='left: " + kinetic + "%'>" + kinetic + "</div></div>";
    html_str += "100</div>";
    html_str += "</td></tr>";
    html_str += "<tr><td class='risk-name'>";
    html_str += lang.equals("de") ? "Verlängerung der QT-Zeit" : "Allongement du temps QT";
    html_str += "</td>";
    html_str += "<td>";
    html_str += "<div class='risk'>0";
    html_str += "<div class='gradient'><div class='pin' style='left: " + qtc + "%'>" + qtc + "</div></div>";
    html_str += "100</div>";
    html_str += "</td></tr>";
    html_str += "<tr><td class='risk-name'>";
    html_str += lang.equals("de") ? "Warnhinweise" : "Avertissements";
    html_str += "</td>";
    html_str += "<td>";
    html_str += "<div class='risk'>0";
    html_str += "<div class='gradient'><div class='pin' style='left: " + warning + "%'>" + warning + "</div></div>";
    html_str += "100</div>";
    html_str += "</td></tr>";
    html_str += "<tr><td class='risk-name'>";
    html_str += lang.equals("de") ? "Serotonerge Effekte" : "Effets sérotoninergiques";
    html_str += "</td>";
    html_str += "<td>";
    html_str += "<div class='risk'>0";
    html_str += "<div class='gradient'><div class='pin' style='left: " + serotonerg + "%'>" + serotonerg + "</div></div>";
    html_str += "100</div>";
    html_str += "</td></tr>";
    html_str += "<tr><td class='risk-name'>";
    html_str += lang.equals("de") ? "Anticholinerge Effekte" : "Effets anticholinergiques";
    html_str += "</td>";
    html_str += "<td>";
    html_str += "<div class='risk'>0";
    html_str += "<div class='gradient'><div class='pin' style='left: " + anticholinergic + "%'>" + anticholinergic + "</div></div>";
    html_str += "100</div>";
    html_str += "</td></tr>";
    html_str += "<tr><td class='risk-name'>";
    html_str += lang.equals("de") ? "Allgemeine Nebenwirkungen" : "Effets secondaires généraux";
    html_str += "</td>";
    html_str += "<td>";
    html_str += "<div class='risk'>0";
    html_str += "<div class='gradient'><div class='pin' style='left: " + adverse + "%'>" + adverse + "</div></div>";
    html_str += "100</div>";
    html_str += "</td></tr>";
    html_str += "</table>";

    return html_str;
  }

  public void updateInteractionsHtml() {
    String basket_html_str = medBasketHtml();
    String interactions_html_str = interactionsHtml();
    String foot_note_html_str = footNoteHtml();

    // Update main interactions html string
    m_interactions_html_str = "<!DOCTYPE html>"
        + "<head><meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />"
        + "<script type=\"text/javascript\">" + m_js_interactions_str + "</script>"
        + m_css_interactions_str + "</head>"
        + "<body><div id=\"interactions\">"
        + basket_html_str + "<br>" + interactions_html_str + "<br>" + foot_note_html_str
        + "</div></body></html>";
    if (htmlUpdated != null) {
      htmlUpdated.run();
    }
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
        basket_html_str = "<div id=\"Medikamentenkorb\"><br><fieldset><legend>Medikamentenkorb</fieldset></legend></div>";
      else if (Constants.appLanguage().equals("fr"))
        basket_html_str = "<div id=\"Medikamentenkorb\"><br><fieldset><legend>Panier des Médicaments</fieldset></legend></div>";

      basket_html_str += "<table id=\"InterTable\" width=\"100%25\">";
      for (Map.Entry<String, Medication> entry1 : m_med_basket.entrySet()) {
        m_code1 = entry1.getValue().getAtcCode().split(";");
        atc_code1 = "k.A.";
        name1 = "k.A.";
        if (m_code1.length>1) {
          atc_code1 = m_code1[0];
          name1 = m_code1[1];
        }
        // Source folder for the images is /res/drawable
        int currentNightMode = mContext.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        String trashIcon =
                currentNightMode == Configuration.UI_MODE_NIGHT_YES ? "trash_icon_white.png" : "trash_icon.png";
        String trash_icon = "<input class=\"delete-button\" type=\"image\" src=\""+trashIcon+"\" onclick=\"deleterow('InterTable',this)\" />";
        basket_html_str += "<tr>"
            + "<td>" + med_counter + "</td>"
            + "<td>" + entry1.getKey() + " </td> "
            + "<td>" + atc_code1 + "</td>"
            + "<td>" + name1 + "</td>"
            + "<td align=\"right\">" + trash_icon + "</td>"
            + "</tr>";
        med_counter++;
      }
      basket_html_str += "</table>";

      if (m_epha_response != null) {
        try {
          String ephaHtml = this.htmlForEpha(m_epha_response);
          basket_html_str += ephaHtml;
        } catch (Exception e){}
      }

      if (Constants.appLanguage().equals("de")) {
        basket_html_str += "<div id=\"Delete_all\">";
        basket_html_str += "<input type=\"button\" value=\"Korb leeren\" onclick=\"deleterow('Delete_all',this)\" />";
        basket_html_str += "<input type=\"button\" value=\"EPha API Details anzeigen\" style=\"cursor: pointer; float:right;\" onclick=\"openEPha()\" />";
        basket_html_str += "</div><br>";
      } else if (Constants.appLanguage().equals("fr")) {
        basket_html_str += "<div id=\"Delete_all\">";
        basket_html_str += "<input type=\"button\" value=\"Tout supprimer\" onclick=\"deleterow('Delete_all',this)\" />";
        basket_html_str += "<input type=\"button\" value=\"Afficher les détails de l'API EPha\" style=\"cursor: pointer; float:right;\" onclick=\"openEPha()\" />";
        basket_html_str += "</div><br>";
      }
    } else {
      // Medikamentenkorb ist leer
      if (Constants.appLanguage().equals("de"))
        basket_html_str = "<div><br>Ihr Medikamentenkorb ist leer.<br><br></div>";
      else if (Constants.appLanguage().equals("fr"))
        basket_html_str = "<div><br>Votre panier de médicaments est vide.<br><br></div>";
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
    m_section_ids_list = new ArrayList<String>();
    // Add table to section titles
    if (Constants.appLanguage().equals("de")) {
      m_section_titles_list.add("Medikamentenkorb");
      m_section_ids_list.add("Medikamentenkorb");
    } else if (Constants.appLanguage().equals("fr")) {
      m_section_titles_list.add("Panier de médicaments");
      m_section_ids_list.add("Medikamentenkorb");
    }

    if (m_med_basket!=null && m_med_basket.size()>1) {
      if (Constants.appLanguage().equals("de"))
        interactions_html_str = "<fieldset><legend>Bekannte Interaktionen</fieldset></legend>";
      else if (Constants.appLanguage().equals("fr"))
        interactions_html_str = "<fieldset><legend>Interactions Connues</fieldset></legend>";

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
                String inter = m_interactions_map.get(atc_code1 + "-" + atc_code2);
                if (inter != null) {
                  inter = inter.replaceAll(atc_code1, entry1.getKey());
                  inter = inter.replaceAll(atc_code2, entry2.getKey());
                  interactions_html_str += (inter + "");
                  // Add title to section title list
                  if (!inter.isEmpty()) {
                    m_section_titles_list.add(entry1.getKey() + "\u2192 " + entry2.getKey());
                    m_section_ids_list.add(entry1.getKey() + "-" + entry2.getKey());
                  }
                }
              }
            }
          }
        }
      }

      // Add note to indicate that there are no interactions
      if (m_section_titles_list.size()<2) {
        if (Constants.appLanguage().equals("de")) {
          interactions_html_str = "<p class=\"paragraph0\">Zur Zeit sind keine Interaktionen zwischen diesen Medikamenten in der EPha.ch-Datenbank vorhanden. "
              + "Weitere Informationen finden Sie in der Fachinformation.</p>"
              + "<div id=\"Delete_all\"><input type=\"button\" value=\"Interaktion melden\" onclick=\"deleterow('Notify_interaction',this)\" />"
              + "</div><br>";
        } else if (Constants.appLanguage().equals("fr")) {
          interactions_html_str = "<p class=\"paragraph0\">Il n’y a aucune information dans la banque de données EPha.ch à propos d’une interaction entre les "
              + "médicaments sélectionnés. Veuillez consulter les informations professionelles.</p>"
              + "<div id=\"Delete_all\"><input type=\"button\" value=\"Signaler une interaction\" onclick=\"deleterow('Notify_interaction',this)\" />"
              + "</div><br>";
        }
      } else
        interactions_html_str += "<br>";

      // Add table to section titles
      if (Constants.appLanguage().equals("de")) {
        m_section_titles_list.add("Farblegende");
        m_section_ids_list.add("Farblegende");
      } else if (Constants.appLanguage().equals("fr")) {
        m_section_titles_list.add("Légende des couleurs");
        m_section_ids_list.add("Farblegende");
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

    if (m_med_basket!=null && m_med_basket.size()>0) {
      if (Constants.appLanguage().equals("de")) {
        legend = "<fieldset><legend>Fussnoten</fieldset></legend>" +
            "<p class=\"footnote\">1. Farblegende: </p>" +
            "<table id=\"Farblegende\" cellpadding=\"3px\" width=\"100%25\">" +
            "<tr bgcolor=\"#caff70\"><td align=\"center\">A</td><td>Keine Massnahmen notwendig</td></tr>" +
            "<tr bgcolor=\"#ffec8b\"><td align=\"center\">B</td><td>Vorsichtsmassnahmen empfohlen</td></tr>" +
            "<tr bgcolor=\"#ffb90f\"><td align=\"center\">C</td><td>Regelmässige Überwachung</td></tr>" +
            "<tr bgcolor=\"#ff82ab\"><td align=\"center\">D</td><td>Kombination vermeiden</td></tr>" +
            "<tr bgcolor=\"#ff6a6a\"><td align=\"center\">X</td><td>Kontraindiziert</td></tr>" +
            "</table>" +
            "<p class=\"footnote\">2. Datenquelle: Public Domain Daten von EPha.ch.</p>" +
            "<p class=\"footnote\">3. Unterstützt durch: IBSA Institut Biochimique SA.</p>";
      } else if (Constants.appLanguage().equals("fr")) {
        legend = "<fieldset><legend>Notes</fieldset></legend>" +
            "<p class=\"footnote\">1. Légende des couleurs: </p>" +
            "<table id=\"Farblegende\" cellpadding=\"3px\" width=\"100%25\">" +
            "<tr bgcolor=\"#caff70\"><td align=\"center\">A</td><td>Aucune mesure nécessaire</td></tr>" +
            "<tr bgcolor=\"#ffec8b\"><td align=\"center\">B</td><td>Mesures de précaution sont recommandées</td></tr>" +
            "<tr bgcolor=\"#ffb90f\"><td align=\"center\">C</td><td>Doit être régulièrement surveillée</td></tr>" +
            "<tr bgcolor=\"#ff82ab\"><td align=\"center\">D</td><td>Eviter la combinaison</td></tr>" +
            "<tr bgcolor=\"#ff6a6a\"><td align=\"center\">X</td><td>Contre-indiquée</td></tr>" +
            "</table>" +
            "<p class=\"footnote\">2. Source des données : données du domaine publique de EPha.ch.</p>" +
            "<p class=\"footnote\">3. Soutenu par : IBSA Institut Biochimique SA.</p>";
      }
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
