dash_list(index, graphs) ::= <<
<table width="95%">
    <thead>
    <tr>
        <th>Title</th>
        <th>Description</th>
        <th>Key</th>
        <th>Tags</th>
        <th>Actions</th>
    </tr>
    </thead>

    <tbody>
    $graphs:{graph |
    <tr>
        <td>$graph.title$</td>
        <td>$graph.description$</td>
        <td>$graph.key.captionWithField$</td>
        <!-- $graph.createTime; format="yyyy-MM-dd HH:mm:ss.SSS"$ -->
        <td>$graph.tagString$</td>
        <td>
            <a href="/console/$index$/usergraph/$graph.id$" title="View" id="dl_permalink"><i class="fi-link"></i></a>
        </td>
    </tr>
    }$
    </tbody>
</table>
>>