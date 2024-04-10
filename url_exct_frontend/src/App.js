import './App.css';
import { Routes, Route, HashRouter as Router } from "react-router-dom";
import Home from './pages/Home';
import Result from './pages/Result';

function App() {
  return (
    <Router>
      <Routes>
        <Route path='/' element={<Home />} />
        <Route path='/result' element={<Result />} />
      </Routes>
    </Router>
  );
}

export default App;
