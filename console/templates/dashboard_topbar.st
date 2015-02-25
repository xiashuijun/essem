dashboard_topbar(index, uid, zoneList) ::= <<
<nav class="top-bar" data-topbar data-options="is_hover: false">
    <ul class="title-area">
        <li class="name">
            <h1>
                <a href="/console/$index$"><img src="$constants.imageBase$/eicon.png"/></a>
            </h1>
        </li>
        <li class="toggle-topbar menu-icon"><a href="#"><span>Menu</span></a></li>
    </ul>
    <section class="top-bar-section">
        <ul class="right">
            <li class="divider"></li>
            <li class="has-dropdown">
                <a href="#">Dashboards</a>
                <ul class="dropdown">
                    <li><a id="topbar_dash" href="#">Recent</a></li>
                </ul>
            </li>
            <li class="divider"></li>
            <li>
                <a>
                    <i class="fi-torso-female"></i>&nbsp;$uid$&nbsp;
                    <i class="fi-clock"></i>&nbsp;<span id="curr-time"></span>
                </a>
            </li>
            <li class="divider"></li>
        </ul>
        <ul class="left">
            $if(zoneList)$
            <li class="divider"></li>
            <li class="has-dropdown">
                <a id="set_tz" href="#"></a>
                <ul class="dropdown">
                    $zoneList:{zone |
                    <li id="tzs_$i$" class="zone-select"><a id="tzn_$i$" class="zone-nav" name="$zone.id$" href="#">$zone.display$</a></li>
                    }$
                </ul>
            </li>
            <li class="divider"></li>
            $endif$
        </ul>
    </section>
</nav>
>>