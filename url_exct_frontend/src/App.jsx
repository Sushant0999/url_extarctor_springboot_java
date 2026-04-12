import './App.css';
import { Routes, Route, HashRouter as Router } from "react-router-dom";
import Home from './pages/Home';
import Result from './pages/Result';
import JobSearch from './pages/JobSearch';
import { AnimatePresence } from 'framer-motion';

function App() {
  return (
    <Router>
      <AnimatePresence mode="wait">
        <Routes>
          <Route path='/' element={<Home />} />
          <Route path='/result' element={<Result />} />
          <Route path='/jobs' element={<JobSearch />} />
        </Routes>
      </AnimatePresence>
    </Router>
  );
}

export default App;
