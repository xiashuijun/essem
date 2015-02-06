metrics(index, app, type, metrics) ::= <<
<ul class="inline-list">
$metrics:{metric |
 <li><a title="View graphs" href="/console/$index$/graphs/$app.name$?name=$metric.name$" target="_new" name="$metric.name$">$metric.name$</a></li>
}$
</ul>
>>