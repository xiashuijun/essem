side_dash(index, tagDashboards) ::= <<
<ul class="side-nav">
$tagDashboards:{dash |
    <li class="app-select">
    <a target="_blank" href="/console/$index$/dash$dash.queryString$">$dash.tagString$</a>
    </li>
}$
</ul>
>>