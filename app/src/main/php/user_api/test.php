<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);
header('Content-Type: application/json');

// Test if the script is accessible
echo json_encode([
    "success" => true,
    "message" => "API is accessible",
    "timestamp" => date('Y-m-d H:i:s')
]);
?>