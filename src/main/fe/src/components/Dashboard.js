import React, { useState, useEffect } from "react";
import { clearTokens, getTokens, refreshAccessToken } from "../util/AuthUtil";
import { useNavigate } from "react-router-dom";

const Dashboard = () => {
  const [wallets, setWallets] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    fetchWallets();
  }, []);

  const fetchWallets = async () => {
    try {
      let accessToken = getTokens().accessToken;

      let response = await fetch("http://localhost:8080/api/address/", {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      });

      if (response.status === 401) {
        accessToken = await refreshAccessToken();
        response = await fetch("http://localhost:8080/api/address/", {
          headers: {
            Authorization: `Bearer ${accessToken}`,
          },
        });
      }

      if (!response.ok) throw new Error("Failed to fetch wallets");

      const data = await response.json();
      setWallets(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setIsLoading(false);
    }
  };

  const handleAddWallet = () => {
    navigate("/add-wallet");
  };

  const handleRefresh = async () => {
    try {
      let accessToken = getTokens().accessToken;

      let response = await fetch(
        "http://localhost:8080/api/address/portfolio/",
        {
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${accessToken}`,
          },
        }
      );

      if (response.status === 401) {
        accessToken = await refreshAccessToken();

        response = await fetch("http://localhost:8080/api/address/portfolio/", {
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${accessToken}`,
          },
        });
      }

      if (!response.ok) throw new Error("Failed to refresh wallets");

      fetchWallets();
    } catch (err) {
      setError(err.message);
    } finally {
      setIsLoading(false);
    }
  };

  const handleRowClick = (address) => {
    navigate(`/transactions/${address}`);
  };

  const logout = () => {
    clearTokens();
    window.location.reload();
  };

  const getCurrencies = () => {
    const currencies = new Set();
    wallets.forEach((wallet) => {
      Object.keys(wallet.balances).forEach((currency) =>
        currencies.add(currency)
      );
    });
    return Array.from(currencies);
  };

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Wallet Addresses</h1>
        <button
          onClick={handleAddWallet}
          className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700"
        >
          Add New Wallet
        </button>
        <button
          onClick={handleRefresh}
          className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700"
        >
          Refresh
        </button>
        <button
          onClick={logout}
          className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700"
        >
          Logout
        </button>
      </div>

      {isLoading && <p>Loading...</p>}
      {error && <p className="text-red-500">{error}</p>}

      {!isLoading && !error && (
        <div className="overflow-x-auto">
          <table className="min-w-full bg-white shadow-md rounded-lg">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Address
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Nickname
                </th>
                {getCurrencies().map((currency) => (
                  <th
                    key={currency}
                    className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                  >
                    {currency}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {wallets.map((wallet) => (
                <tr
                  key={wallet.address}
                  onClick={() => handleRowClick(wallet.id)}
                  className="cursor-pointer hover:bg-gray-50"
                >
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                    {wallet.address}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {wallet.nickname}
                  </td>
                  {getCurrencies().map((currency) => (
                    <td
                      key={currency}
                      className="px-6 py-4 whitespace-nowrap text-sm text-gray-500"
                    >
                      {wallet.balances[currency] || "0"}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default Dashboard;
