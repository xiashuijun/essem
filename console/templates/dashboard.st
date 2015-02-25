dashboard(uid, index, graphs, dash, zoneList) ::= <<
<!doctype html>
<html class="no-js" lang="en">
$head()$
<body>
$dashboard_topbar(index, uid, zoneList)$
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

    function renderUpdate() {
     if(autoUpdate) {
      $graphs:{graph |
       render_$graph.sid$();
      }$
     }
    }

    renderAll();

    $if(dash.autoUpdate)$
    var autoUpdate = true;
    setInterval(renderUpdate, $dash.autoUpdateSeconds$ * 1000);
    $endif$

    $if(zoneList)$
     $zoneList:{zone |
        if(offsetMatchesLocal('$zone.id$')) setCurrentTimezone('$zone.display$');
     }$
    $endif$

    var currTimeField = jQuery('#curr-time');
    var currTimeFormat = 'YYYY-MM-DD HH:mm:ss Z';
    currTimeField.text(moment().format(currTimeFormat));

</script>
</body>
</html>
>>