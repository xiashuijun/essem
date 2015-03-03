dash_list(index, graphs) ::= <<
<table width="95%">
    <thead>
    <tr>
        <th>Title</th>
        <th>Description</th>
        <th>Key</th>
        <th>Id</th>
        <th>Tags</th>
        <th>Actions</th>
    </tr>
    </thead>

    <tbody>
    $graphs:{graph |
    <tr id="gr_$graph.id$">
        <td>$graph.title$</td>
        <td>$graph.description$</td>
        <td>$graph.key.captionWithField$</td>
        <td><a  href="/console/$index$/usergraph/$graph.id$" title="View">$graph.sid$</a></td>
        <!-- $graph.createTime; format="yyyy-MM-dd HH:mm:ss.SSS"$ -->
        <td>
            $graph.tagsForQuery:{tag |
            <a target="_blank" href="/console/attribyte/dash?tag=$tag$">$tag$</a>&nbsp;
            }$
        </td>
        <td>
            <a href="/console/$index$/usergraph/$graph.id$" title="View" id="dl_permalink"><i class="fi-link"></i></a>
            &nbsp;&nbsp;<a href="#" title="Delete" id="dg_$graph.id$"><i class="fi-page-delete"></i></a>
        </td>
    </tr>
    }$
    </tbody>
</table>
<script type="application/javascript">
 function triggerGraphDelete(event) {
  event.preventDefault();
  if(confirm("Permanently delete this graph?")) {deleteGraph('$index$', getId(this.id), 'gr_' + getId(this.id));}
 }
$graphs:{graph |
 jQuery('#dg_$graph.id$').click(triggerGraphDelete);
}$
</script>
>>