import React, { useState, useEffect } from "react";
import { useParams } from "react-router-dom";
import { getTokens, refreshAccessToken } from "../util/AuthUtil";
import { useNavigate } from "react-router-dom";

const TransactionHistory = () => {
  const { address } = useParams();
  const [transactions, setTransactions] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    fetchTransactions();
  }, [address]);

  const fetchTransactions = async () => {
    try {
      let accessToken = getTokens().accessToken;

      let response = await fetch(
        `http://localhost:8080/api/address/portfolio/transactions/`,
        {
          method: "POST",
          headers: {
            Authorization: `Bearer ${accessToken}`,
            "Content-Type": "application/json",
          },
          body: JSON.stringify({ address }),
        }
      );

      if (response.status === 401) {
        accessToken = await refreshAccessToken();

        response = await fetch(
          `http://localhost:8080/api/address/portfolio/transactions/`,
          {
            method: "POST",
            headers: {
              Authorization: `Bearer ${accessToken}`,
              "Content-Type": "application/json",
            },
            body: JSON.stringify({ address }),
          }
        );
      }

      if (!response.ok) throw new Error("Failed to fetch transactions");

      const data = await response.json();
      setTransactions(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setIsLoading(false);
    }
  };

  const handleRefresh = async () => {
    try {
      let accessToken = getTokens().accessToken;

      let response = await fetch(
        "http://localhost:8080/api/address/portfolio/transactions/refresh",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${accessToken}`,
          },
          body: JSON.stringify({ address: address }),
        }
      );

      // If token expired, refresh and retry
      if (response.status === 401) {
        accessToken = await refreshAccessToken();

        response = await fetch(
          "http://localhost:8080/api/address/portfolio/transactions/refresh",
          {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              Authorization: `Bearer ${accessToken}`,
            },
            body: JSON.stringify({ id: address }),
          }
        );
      }

      if (!response.ok) {
        window.alert(
          "Failed to refresh transactions. No transactions found",
          "danger"
        );
      }

      fetchTransactions();
    } catch (err) {
      setError(err.message);
    } finally {
      setIsLoading(false);
    }
  };

  const goBack = () => {
    navigate("/dashboard");
  };

  const formatDate = (timestamp) => {
    return new Date(timestamp).toLocaleString();
  };

  return (
    <div className="container mx-auto px-4 py-8">
      <button
        onClick={goBack}
        className="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded"
      >
        Back
      </button>
      <button
        onClick={handleRefresh}
        className="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded ms-4"
      >
        Refresh
      </button>
      <h2 className="text-2xl font-bold mb-6">Transactions for {address}</h2>
      {isLoading && <p>Loading...</p>}
      {error && <p className="text-red-500">{error}</p>}

      {!isLoading && !error && (
        <div className="overflow-x-auto">
          <table className="min-w-full bg-white shadow-md rounded-lg">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  Stablecoin
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  Value
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  Hash
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  Timestamp
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {transactions.map((tx) => (
                <tr key={tx.transactionId}>
                  <td className="px-6 py-4 whitespace-nowrap">
                    {tx.stablecoin.name}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    {tx.value.toFixed(2)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">{tx.hash}</td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    {formatDate(tx.timestamp)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default TransactionHistory;
