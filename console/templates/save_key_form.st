save_key_form(id, key, title) ::= <<

<form action="POST" id="save-key-form">
    <div class="row">
        <div class="small-12 colunms">
            <h6 class="subheader" id="save-key-form-title">
                $if(key.hasHost)$
                <h5>$key.application$&nbsp;&raquo;$key.host$&nbsp;&raquo;&nbsp;$key.name$&nbsp;&raquo;&nbsp;$key.field$</h5>
                $else$
                <h5>$key.application$&nbsp;&raquo;&nbsp;$key.name$&nbsp;&raquo;&nbsp;$key.field$</h5>
                $endif$
            </h6>
        </div>
    </div>
    <div class="row collapse">
        <div class="small-7 columns">
            <label><b>Description</b></label>
            <input id="skey-title" type="text" name="title" value="$title$"/>

            <label><b>X-Axis Label</b></label>
            <input id="skey-xlabel" type="text" name="xlabel" value=""/>

            <label><b>Y-Axis Label</b></label>
            <input id="skey-ylabel" type="text" name="ylabel" value=""/>

        </div>
        <div class="small-2 columns">
            <input type="submit" class="button postfix" value="Save"/>
        </div>
    </div>
    <input type="hidden" name="application" value="$key.application$"/>
    <input type="hidden" name="host" value="$key.host$"/>
    <input type="hidden" name="instance" value="$key.instance$"/>
    <input type="hidden" name="name" value="$key.name$"/>
    <input type="hidden" name="field" value="$key.field$"/>
    <input type="hidden" name="id" value="$id$"/>
</form>
>>