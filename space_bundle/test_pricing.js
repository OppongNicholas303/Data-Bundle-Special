async function test() {
  try {
    const loginRes = await fetch('http://localhost:8080/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ phoneNumber: '0240000000', password: 'password' })
    });
    const loginData = await loginRes.json();
    const token = loginData.data.accessToken;
    console.log('Token:', token);

    const putRes = await fetch('http://localhost:8080/api/admin/results-checker/pricing/VoucherPricePlatformWaecNew', {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + token
      },
      body: JSON.stringify({ retailPrice: 55 })
    });
    console.log('PUT Response:', await putRes.json());
    
    const getRes = await fetch('http://localhost:8080/api/results-checker/pricing');
    console.log('GET Response:', await getRes.json());
  } catch(err) {
    console.error('Error:', err);
  }
}
test();
