<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>New Lower Bicutan Portal</title>
<style>
body {
    font-family: Arial, sans-serif;
    background: linear-gradient(to bottom, #fce4ec, #ffffff);
    margin: 0;
    padding: 0;
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100vh;
}
.container {
    background-color: white;
    border-radius: 8px;
    box-shadow: 0 0 20px rgba(0, 0, 0, 0.1);
    padding: 30px;
    width: 400px;
    text-align: center;
}
h1 {
    color: #e74c3c;
    margin-bottom: 20px;
    font-size: 2rem;
}
.message {
    color: #27ae60;
    font-size: 16px;
    margin: 10px 0;
}
.error-message {
    color: #e74c3c;
    font-size: 16px;
    margin: 10px 0;
}
form {
    display: flex;
    flex-direction: column;
    align-items: center;
}
input, select, button {
    width: 80%;
    padding: 12px;
    margin-bottom: 15px;
    border-radius: 4px;
    border: 1px solid #ddd;
    font-size: 16px;
}
button {
    background-color: #f0e4d7;
    cursor: pointer;
    transition: background-color 0.3s ease;
}
button:hover {
    background-color: #e1d0a1;
}
.cancel {
    background-color: #e74c3c;
    color: white;
}
</style>
</head>
<body>
<div class="container">
    <h1>NLB Center Portal</h1>
    <?php if (!empty($message)): ?>
        <div class="message"><?php echo $message; ?></div>
    <?php endif; ?>
    <form method="POST">
        <label for="name">Name</label>
        <input type="text" id="name" name="name" value="<?php echo htmlspecialchars($name ?? ''); ?>" required>
        
        <label for="appointment_type">Type of Appointment</label>
        <select id="appointment_type" name="appointment_type" required>
            <option value="">--Select Appointment Type--</option>
            <option value="Dental" <?php echo ($appointment_type == 'Dental') ? 'selected' : ''; ?>>Dental</option>
            <option value="Medical Check-up" <?php echo ($appointment_type == 'Medical Check-up') ? 'selected' : ''; ?>>Medical Check-up</option>
            <option value="Eye Consultation" <?php echo ($appointment_type == 'Eye Consultation') ? 'selected' : ''; ?>>Eye Consultation</option>
            <option value="Pre-Natal" <?php echo ($appointment_type == 'Pre-Natal') ? 'selected' : ''; ?>>Pre-Natal</option>
            <option value="TB-Dots" <?php echo ($appointment_type == 'TB-Dots') ? 'selected' : ''; ?>>TB-Dots</option>
        </select>

        <label for="time">Appointment Time</label>
        <input type="datetime-local" id="time" name="time" value="<?php echo htmlspecialchars($time ?? ''); ?>" required>

        <label for="expiration_date">Expiration Date</label>
        <input type="datetime-local" id="expiration_date" name="expiration_date" value="<?php echo htmlspecialchars($expiration_date ?? ''); ?>" required>

        <button type="submit" name="action" value="submit">Submit Appointment</button>
        <button type="submit" name="action" value="cancel" class="cancel">Cancel Appointment</button>
        <button type="submit" name="action" value="move">Move Appointment</button>
    </form>
</div>
</body>
</html>
