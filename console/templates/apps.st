apps(index, appList) ::= <<
<ul class="side-nav">
$appList:{app |
    <li id="mapp_$app.name$" class="app-select">
        <a class="app-nav" id="app_$app.name$" href="#" name="$app.name$">
            $app.name$ $if(app.multiHost)$ ($app.hostCount$ hosts) $endif$
        </a>
    </li>
}$
</ul>
>>