<?php

// Generate a random username for the connecting client
function randomUsername() {

    $FIRST_NAMES = array(
        'Jason'
    );

    $LAST_NAMES = array(
        'Lu'
    );

    // Choose random components of username and return it

    $fn = $FIRST_NAMES[array_rand($FIRST_NAMES)];
    $ln = $LAST_NAMES[array_rand($LAST_NAMES)];
    
    return $fn . $ln;
}