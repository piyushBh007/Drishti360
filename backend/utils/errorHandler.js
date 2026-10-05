function handleApiError(res, err) {
    console.error('[API ERROR]', err.message || err);

    let status = 500;
    let code = 'INTERNAL_SERVER_ERROR';
    let message = 'Something went wrong on the server. Please try again.';

    // PostgreSQL Unique Violation
    if (err.code === '23505') {
        status = 409; // Conflict
        if (err.constraint === 'users_employee_id_key') {
            code = 'DUPLICATE_EMPLOYEE_ID';
            message = 'Employee ID already exists. Please use a different Employee ID.';
        } else if (err.constraint === 'users_username_key') {
            code = 'DUPLICATE_USERNAME';
            message = 'Username already exists. Please choose another username.';
        } else if (err.constraint === 'users_email_key') {
            code = 'DUPLICATE_EMAIL';
            message = 'Email address already exists. Please use another email.';
        } else if (err.constraint === 'ngos_registration_no_key') {
            code = 'DUPLICATE_REGISTRATION';
            message = 'An NGO with this registration number already exists.';
        } else {
            code = 'CONFLICT';
            message = 'The information provided conflicts with an existing record.';
        }
    } 
    // PostgreSQL Not Null Violation
    else if (err.code === '23502') {
        status = 400;
        code = 'VALIDATION_ERROR';
        message = `Missing required field: ${err.column}`;
    }

    res.status(status).json({
        success: false,
        error: {
            code: code,
            message: message
        }
    });
}

function sendError(res, status, code, message) {
    res.status(status).json({
        success: false,
        error: {
            code: code,
            message: message
        }
    });
}

module.exports = {
    handleApiError,
    sendError
};
