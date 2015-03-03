dashboard(uid, index, graphs, dash, custom, zoneList, zoneDashboards, tagDashboards, toggleAuto) ::= <<
<!doctype html>
<html class="no-js" lang="en">
$head()$
<body>
$dashboard_topbar(index, uid, zoneList, zoneDashboards, tagDashboards, toggleAuto)$
$if(custom)$
$custom$
$else$
<div class="row" $if(dash.displayGrid)$style="min-width:95% !important;"$endif$>
    <div id="content" class="large-12 columns">
        $if(dash.displayGrid)$
         <ul class="small-block-grid-$dash.smallBlockGridColumns$ large-block-grid-$dash.largeBlockGridColumns$">
         $graphs:{graph |
            <li><div id="gd_$graph.sid$"></div></li>
         }$
         </ul>
        $else$
         $graphs:{graph |
            <div class="row"><div id="gd_$graph.sid$" class="large-12 columns"></div></div>
         }$
        $endif$
    </div>
</div>
$endif$
<script type="application/javascript">
    jQuery(document).foundation();
    $graphs:{graph |
      $dashboard_graph(index, graph, dash)$;
    }$

    function renderAll() {
     $graphs:{graph |
      render_$graph.sid$();
     }$
    }

    renderAll();

    var currTimeField = jQuery('#curr-time');
    var currTimeFormat = 'YYYY-MM-DD HH:mm:ss Z';
    currTimeField.text(moment().format(currTimeFormat));

    $if(dash.autoUpdate)$

    var checkSeconds = 5;
    var secondsLeft = $dash.autoUpdateSeconds$;

    function renderUpdate() {
        if(secondsLeft <= 0) {
            $graphs:{graph |
             render_$graph.sid$();
            }$
            secondsLeft = $dash.autoUpdateSeconds$;
            jQuery('#next_auto_update').html("Now");
            currTimeField.text(moment().format(currTimeFormat));
        } else {
            jQuery('#next_auto_update').html(secondsLeft + "s");
            secondsLeft = secondsLeft - checkSeconds;
        }
    }

    jQuery('#auto_update_button').addClass("active");
    jQuery('#next_auto_update').html($dash.autoUpdateSeconds$ + "s");

    setInterval(renderUpdate, checkSeconds * 1000);
    $else$
    jQuery('#next_auto_update').html("Off");
    $endif$

    $if(dash.displayTZ.display)$
        setCurrentTimezone('$dash.displayTZ.display$');
    $elseif(zoneList)$
    $zoneList:{zone |
        if(offsetMatchesLocal('$zone.id$')) setCurrentTimezone('$zone.display$');
    }$
    $endif$


</script>
</body>
</html>
>>