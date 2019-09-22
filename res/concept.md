        field (* = required)                 use
------------------------------------------------------------------
    LEVEL  
        name *             name of level (should be identical to file name)  
        states             number of states the level can be in, >= 1 (defaults to 1 if missing)  
        background         background graphics  
                             - missing -> uses $name$.png if 1 state, else $name$_$state$.png  
                             - "$file$" -> uses $file$.png  
                             - ["$file0$", "$file1$", ...] uses $file[state]$.png  
        music *            background track  
        objects *          objects in the level  
    
    OBJECT  
        name *             $level_name$.$name$ used as reference in scripts  
        visible            if object is visible and clickable (default value; value can be changed by scripts) (defaults to true if missing)  
        states             number of states this object can be in, >= 1 (defaults to 1 if missing)  
        sprite             object sprites  
                             - missing -> uses $level_name$.$name$.png if 1 state, else $level_name$.$name$_$state$.png (not allowed if $name$ is missing)  
                             - $file$ -> uses $file$.png  
                             - ["$file0$", "$file1$", ...] uses $file[state]$.png  
        x *                x-coordinate of center of object in level  
        y *                y-coordinate of center of object in level  
        script *           script to be executed when clicked  
                             - string -> executes string as script  
                             - array of strings -> executes $state$th string in array as script