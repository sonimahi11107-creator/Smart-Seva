package com.example.smartseva;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.webkit.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.*;

public class MapViewActivity extends AppCompatActivity {

    WebView webViewMap;
    TextView tvMapSubtitle, tvMapTotal, tvMapCriticalCount,
            tvMapOpenCount, tvMapResolvedCount;
    Button btnBackMap, btnMapAll, btnMapCritical,
            btnMapFood, btnMapMedical, btnMapEducation, btnMapOpen;
    LinearLayout layoutSelectedNeed;
    TextView tvSelectedTitle, tvSelectedLocation, tvSelectedUrgency;
    Button btnSelectedApply, btnSelectedClose;

    String currentFilter = "all";
    List<NeedPin> allPins = new ArrayList<>();
    NeedPin selectedPin = null;

    // ── Need Pin Model ──
    static class NeedPin {
        String id, title, category, urgency, status, location, desc;
        double lat, lon;
        int volunteers;

        NeedPin(String id, String title, String category, String urgency,
                String status, String location, String desc,
                double lat, double lon, int volunteers) {
            this.id         = id;
            this.title      = title;
            this.category   = category;
            this.urgency    = urgency;
            this.status     = status;
            this.location   = location;
            this.desc       = desc;
            this.lat        = lat;
            this.lon        = lon;
            this.volunteers = volunteers;
        }

        String getMarkerColor() {
            if (urgency.contains("Critical")) return "#C62828";
            if (urgency.contains("Moderate")) return "#F57F17";
            return "#2E7D32";
        }

        String getMarkerEmoji() {
            switch (category) {
                case "Food Distribution": return "🍽️";
                case "Medical Help":      return "🏥";
                case "Education":         return "📚";
                case "Environment":       return "🌱";
                case "Disaster Relief":   return "🆘";
                default:                  return "📌";
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_view);

        // ── Views ──
        webViewMap          = findViewById(R.id.webViewCommunityMap);
        tvMapSubtitle       = findViewById(R.id.tvMapSubtitle);
        tvMapTotal          = findViewById(R.id.tvMapTotal);
        tvMapCriticalCount  = findViewById(R.id.tvMapCriticalCount);
        tvMapOpenCount      = findViewById(R.id.tvMapOpenCount);
        tvMapResolvedCount  = findViewById(R.id.tvMapResolvedCount);
        btnBackMap          = findViewById(R.id.btnBackMap);
        btnMapAll           = findViewById(R.id.btnMapAll);
        btnMapCritical      = findViewById(R.id.btnMapCritical);
        btnMapFood          = findViewById(R.id.btnMapFood);
        btnMapMedical       = findViewById(R.id.btnMapMedical);
        btnMapEducation     = findViewById(R.id.btnMapEducation);
        btnMapOpen          = findViewById(R.id.btnMapOpen);
        layoutSelectedNeed  = findViewById(R.id.layoutSelectedNeed);
        tvSelectedTitle     = findViewById(R.id.tvSelectedTitle);
        tvSelectedLocation  = findViewById(R.id.tvSelectedLocation);
        tvSelectedUrgency   = findViewById(R.id.tvSelectedUrgency);
        btnSelectedApply    = findViewById(R.id.btnSelectedApply);
        btnSelectedClose    = findViewById(R.id.btnSelectedClose);

        // ── WebView Setup ──
        WebSettings settings = webViewMap.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setDomStorageEnabled(true);

        // ── JavaScript Interface ──
        webViewMap.addJavascriptInterface(new MapJSInterface(), "Android");
        webViewMap.setWebViewClient(new WebViewClient());

        // ── Listeners ──
        btnBackMap.setOnClickListener(v -> finish());
        btnSelectedClose.setOnClickListener(v ->
                layoutSelectedNeed.setVisibility(android.view.View.GONE));

        btnSelectedApply.setOnClickListener(v -> {
            if (selectedPin != null) {
                Intent intent = new Intent(this, TaskDetailActivity.class);
                intent.putExtra("taskTitle",      selectedPin.title);
                intent.putExtra("taskDesc",       selectedPin.desc);
                intent.putExtra("taskCategory",   selectedPin.category);
                intent.putExtra("taskUrgency",    selectedPin.urgency);
                intent.putExtra("taskLocation",   selectedPin.location);
                intent.putExtra("taskNGO",        "Smart Seva NGO");
                intent.putExtra("taskVolunteers", selectedPin.volunteers);
                startActivity(intent);
            }
        });

        btnMapAll.setOnClickListener(v -> {
            currentFilter = "all";
            setFilterActive(btnMapAll);
            loadMap();
        });
        btnMapCritical.setOnClickListener(v -> {
            currentFilter = "critical";
            setFilterActive(btnMapCritical);
            loadMap();
        });
        btnMapFood.setOnClickListener(v -> {
            currentFilter = "food";
            setFilterActive(btnMapFood);
            loadMap();
        });
        btnMapMedical.setOnClickListener(v -> {
            currentFilter = "medical";
            setFilterActive(btnMapMedical);
            loadMap();
        });
        btnMapEducation.setOnClickListener(v -> {
            currentFilter = "education";
            setFilterActive(btnMapEducation);
            loadMap();
        });
        btnMapOpen.setOnClickListener(v -> {
            currentFilter = "open";
            setFilterActive(btnMapOpen);
            loadMap();
        });

        // ── Load Data + Map ──
        loadSamplePins();
        updateStats();
        loadMap();
    }

    // ═══════════════════════════════════════
    // SAMPLE DATA
    // ═══════════════════════════════════════

    void loadSamplePins() {
        allPins.clear();
        // Firebase teammate yahan Firestore se real data load karega

        allPins.add(new NeedPin("N001",
                "Food Distribution Drive",
                "Food Distribution", "🔴 Critical (24 hrs)",
                "Open", "Raipur, CG",
                "150 families need food packets urgently.",
                21.2514, 81.6296, 10));

        allPins.add(new NeedPin("N002",
                "Free Medical Camp",
                "Medical Help", "🔴 Critical (24 hrs)",
                "Open", "Raipur Civil Lines",
                "Medical assistance needed for elderly patients.",
                21.2600, 81.6350, 5));

        allPins.add(new NeedPin("N003",
                "Teaching Underprivileged Kids",
                "Education", "🟡 Moderate (1 week)",
                "Assigned", "Dharampura, Raipur",
                "30 children need free tutoring support.",
                21.2450, 81.6200, 3));

        allPins.add(new NeedPin("N004",
                "Tree Plantation Drive",
                "Environment", "🟢 Normal",
                "Open", "Bilaspur, CG",
                "Plant 200 trees in industrial area.",
                22.0797, 82.1409, 15));

        allPins.add(new NeedPin("N005",
                "Disaster Relief - Flood",
                "Disaster Relief", "🔴 Critical (24 hrs)",
                "Open", "Korba, CG",
                "Flood affected 50 families need shelter.",
                22.3595, 82.7501, 20));

        allPins.add(new NeedPin("N006",
                "Women Skill Development",
                "Education", "🟡 Moderate (1 week)",
                "In Progress", "Durg, CG",
                "Skill training for 25 women.",
                21.1904, 81.2849, 5));

        allPins.add(new NeedPin("N007",
                "Water Distribution",
                "Medical Help", "🟡 Moderate (1 week)",
                "Open", "Rajnandgaon, CG",
                "Clean water needed for 200 households.",
                20.7000, 81.0300, 8));

        allPins.add(new NeedPin("N008",
                "Senior Citizens Care",
                "Medical Help", "🟢 Normal",
                "Resolved", "Bhilai, CG",
                "Regular visits for 10 elderly living alone.",
                21.1938, 81.3509, 4));
    }

    // ═══════════════════════════════════════
    // STATS
    // ═══════════════════════════════════════

    void updateStats() {
        List<NeedPin> filtered = getFilteredPins();
        int critical = 0, open = 0, resolved = 0;
        for (NeedPin p : allPins) {
            if (p.urgency.contains("Critical")) critical++;
            if (p.status.equals("Open"))        open++;
            if (p.status.equals("Resolved"))    resolved++;
        }
        tvMapTotal.setText(String.valueOf(filtered.size()));
        tvMapCriticalCount.setText(String.valueOf(critical));
        tvMapOpenCount.setText(String.valueOf(open));
        tvMapResolvedCount.setText(String.valueOf(resolved));
        tvMapSubtitle.setText(filtered.size() + " needs on map");
    }

    List<NeedPin> getFilteredPins() {
        List<NeedPin> result = new ArrayList<>();
        for (NeedPin p : allPins) {
            switch (currentFilter) {
                case "critical":
                    if (p.urgency.contains("Critical")) result.add(p); break;
                case "food":
                    if (p.category.equals("Food Distribution")) result.add(p); break;
                case "medical":
                    if (p.category.equals("Medical Help")) result.add(p); break;
                case "education":
                    if (p.category.equals("Education")) result.add(p); break;
                case "open":
                    if (p.status.equals("Open")) result.add(p); break;
                default:
                    result.add(p); break;
            }
        }
        return result;
    }

    // ═══════════════════════════════════════
    // MAP HTML
    // ═══════════════════════════════════════

    void loadMap() {
        updateStats();
        List<NeedPin> pins = getFilteredPins();

        // Build markers JS
        StringBuilder markers = new StringBuilder();
        for (NeedPin pin : pins) {
            String color  = pin.getMarkerColor();
            String emoji  = pin.getMarkerEmoji();
            String status = pin.status;

            // Status ring color
            String ringColor;
            switch (status) {
                case "Open":        ringColor = "#1565C0"; break;
                case "Assigned":    ringColor = "#F57F17"; break;
                case "In Progress": ringColor = "#6A1B9A"; break;
                case "Resolved":    ringColor = "#2E7D32"; break;
                default:            ringColor = "#888888"; break;
            }

            markers.append(String.format(
                    "addMarker(%f, %f, '%s', '%s', '%s', '%s', '%s', '%s', %d, '%s');\n",
                    pin.lat, pin.lon,
                    pin.id,
                    pin.title.replace("'", "\\'"),
                    color,
                    emoji,
                    pin.urgency.replace("'", "\\'"),
                    pin.status,
                    pin.volunteers,
                    ringColor
            ));
        }

        String html = "<!DOCTYPE html><html><head>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>" +
                "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>" +
                "<style>" +
                "html,body,#map{width:100%;height:100%;margin:0;padding:0;}" +
                ".custom-marker{" +
                "  display:flex;align-items:center;justify-content:center;" +
                "  border-radius:50%;font-size:20px;" +
                "  border:3px solid white;box-shadow:0 2px 8px rgba(0,0,0,0.4);" +
                "}" +
                ".pulse{" +
                "  animation:pulse 1.5s infinite;" +
                "}" +
                "@keyframes pulse{" +
                "  0%{box-shadow:0 0 0 0 rgba(198,40,40,0.6);}" +
                "  70%{box-shadow:0 0 0 12px rgba(198,40,40,0);}" +
                "  100%{box-shadow:0 0 0 0 rgba(198,40,40,0);}" +
                "}" +
                ".leaflet-popup-content{font-family:sans-serif;}" +
                "</style>" +
                "</head><body>" +
                "<div id='map'></div>" +
                "<script>" +
                "var map = L.map('map').setView([21.5, 82.0], 7);" +
                "L.tileLayer('https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png',{" +
                "  attribution:'© OpenStreetMap © CARTO'," +
                "  subdomains:'abcd'," +
                "  maxZoom:19" +
                "}).addTo(map);" +

                // Custom marker function
                "function addMarker(lat, lon, id, title, color, emoji, urgency, status, volunteers, ringColor) {" +
                "  var isPulse = urgency.indexOf('Critical') !== -1;" +
                "  var icon = L.divIcon({" +
                "    html: '<div class=\"custom-marker' + (isPulse ? ' pulse' : '') + '\" " +
                "           style=\"background:' + color + ';width:36px;height:36px;border-color:' + ringColor + '\">' + emoji + '</div>'," +
                "    iconSize:[36,36]," +
                "    iconAnchor:[18,18]," +
                "    className:''" +
                "  });" +
                "  var statusBadge = '<span style=\"background:' + ringColor + ';color:white;padding:2px 8px;border-radius:4px;font-size:11px;\">' + status + '</span>';" +
                "  var marker = L.marker([lat, lon], {icon:icon}).addTo(map);" +
                "  marker.bindPopup(" +
                "    '<div style=\"min-width:180px\">' +" +
                "    '<b style=\"font-size:14px\">' + emoji + ' ' + title + '</b><br>' +" +
                "    '<span style=\"color:#888;font-size:12px\">📍 ' + (lat.toFixed(4)) + ', ' + (lon.toFixed(4)) + '</span><br><br>' +" +
                "    statusBadge + '<br><br>' +" +
                "    '<span style=\"color:#555;font-size:12px\">👥 ' + volunteers + ' volunteers needed</span><br>' +" +
                "    '<button onclick=\"Android.onMarkerClick(\\'' + id + '\\')\" " +
                "      style=\"margin-top:8px;width:100%;padding:8px;background:#1A1A1A;color:white;border:none;border-radius:4px;font-size:13px;cursor:pointer\">" +
                "      View Details</button>' +" +
                "    '</div>'" +
                "  );" +
                "}" +

                // Legend
                "var legend = L.control({position:'bottomright'});" +
                "legend.onAdd = function(map){" +
                "  var div = L.DomUtil.create('div','');" +
                "  div.style.background='white';" +
                "  div.style.padding='8px';" +
                "  div.style.borderRadius='8px';" +
                "  div.style.fontSize='12px';" +
                "  div.innerHTML='<b>Legend</b><br>" +
                "    <span style=color:#C62828>● Critical</span><br>" +
                "    <span style=color:#F57F17>● Moderate</span><br>" +
                "    <span style=color:#2E7D32>● Normal</span>';" +
                "  return div;" +
                "};" +
                "legend.addTo(map);" +

                // Add all markers
                markers.toString() +

                "</script></body></html>";

        webViewMap.loadDataWithBaseURL(
                "https://openstreetmap.org", html, "text/html", "UTF-8", null);
    }

    // ═══════════════════════════════════════
    // JS INTERFACE — Marker click se Android ko call karo
    // ═══════════════════════════════════════

    class MapJSInterface {
        @android.webkit.JavascriptInterface
        public void onMarkerClick(String pinId) {
            runOnUiThread(() -> {
                for (NeedPin pin : allPins) {
                    if (pin.id.equals(pinId)) {
                        selectedPin = pin;
                        showSelectedNeed(pin);
                        break;
                    }
                }
            });
        }
    }

    void showSelectedNeed(NeedPin pin) {
        layoutSelectedNeed.setVisibility(android.view.View.VISIBLE);
        tvSelectedTitle.setText(pin.getMarkerEmoji() + " " + pin.title);
        tvSelectedLocation.setText("📍 " + pin.location);
        tvSelectedUrgency.setText(pin.urgency);

        int urgencyColor;
        if (pin.urgency.contains("Critical"))     urgencyColor = Color.parseColor("#C62828");
        else if (pin.urgency.contains("Moderate"))urgencyColor = Color.parseColor("#F57F17");
        else                                       urgencyColor = Color.parseColor("#2E7D32");
        tvSelectedUrgency.setBackgroundColor(urgencyColor);

        // Hide apply if resolved
        if (pin.status.equals("Resolved")) {
            btnSelectedApply.setText("✅ Resolved");
            btnSelectedApply.setEnabled(false);
            btnSelectedApply.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#AAAAAA")));
        } else {
            btnSelectedApply.setText("Apply Now");
            btnSelectedApply.setEnabled(true);
            btnSelectedApply.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#1A1A1A")));
        }
    }

    // ═══════════════════════════════════════
    // FILTER TAB
    // ═══════════════════════════════════════

    void setFilterActive(Button active) {
        Button[] btns = {btnMapAll, btnMapCritical, btnMapFood,
                btnMapMedical, btnMapEducation, btnMapOpen};
        for (Button b : btns) {
            b.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
            b.setTextColor(Color.parseColor("#1A1A1A"));
        }
        active.setBackgroundTintList(
                ColorStateList.valueOf(Color.parseColor("#1A1A1A")));
        active.setTextColor(Color.WHITE);
    }
}