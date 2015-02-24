dashboard(uid, index, title, tags, graphs, withTitles, withStats, tz, width, height) ::= <<
<!doctype html>
<html class="no-js" lang="en">
$head()$
<body>
<div class="row">
    <div id="content" class="large-12 columns">
        $graphs:{graph |
        <div class="row">
            <div class="large-12 columns" id="gd_$graph.id$"></div>
        </div>
        <div class="row">
            <div class="large-12 columns" id="gds_$graph.id$"></div>
        </div>
        }$
    </div>
</div>
<script type="application/javascript">
    jQuery(document).foundation();
    $graphs:{graph |
      $dashboard_graph(index, graph, tz)$;
    }$
</script>
</body>
</html>
>>